package dev.catfi.reporting.application;

import static org.assertj.core.api.Assertions.*;
import dev.catfi.aws.*;
import dev.catfi.common.domain.*;
import dev.catfi.reporting.adapter.kafka.ReportingEventProcessor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class DurableReportingTest {
  private final UUID property=UUID.randomUUID(),assessment=UUID.randomUUID(),compensation=UUID.randomUUID();
  private DamageAssessment damage(Instant time,UUID id) {return DamageAssessment.builder().propertyId(property).assessmentId(id).damageAmount(new BigDecimal("20.01")).assessedAt(time).build();}
  private CompensationResult compensation(){return CompensationResult.builder().propertyId(property).resultId(compensation).damageAssessmentId(assessment).totalCompensation(new BigDecimal("10.01")).amountToPay(new BigDecimal("5.01")).amountToInvest(new BigDecimal("5")).build();}
  private Payment payment(){return Payment.builder().propertyId(property).compensationResultId(compensation).amount(new BigDecimal("5.01")).build();}
  private InvestmentDecision investment(){return InvestmentDecision.builder().propertyId(property).compensationResultId(compensation).allocation(Map.of("GOLD",new BigDecimal("5"))).build();}
  @Test void retainsUnfinishedCorrelationAcrossNewServiceInstancesAndDeduplicates() {
    var store=new MemoryReportStore();var first=new ReportingEventProcessor(new ReportingService(store));
    first.handlePayment(payment());first.handleCompensation(compensation());
    var service=new ReportingService(store);var restarted=new ReportingEventProcessor(service);
    restarted.handleInvestment(investment());var d=damage(Instant.parse("2026-01-02T00:00:00Z"),assessment);restarted.handleDamage(d);
    long version=store.find(property.toString()).orElseThrow().version();restarted.handleDamage(d);
    assertThat(store.find(property.toString()).orElseThrow().version()).isEqualTo(version);
    assertThat(service.getReport(property).getStatus()).isEqualTo("REPORTED");
    assertThat(service.getReport(property).getActualPayout()).isEqualByComparingTo("5.01");
    restarted.handleDamage(damage(Instant.parse("2026-01-01T00:00:00Z"),UUID.randomUUID()));
    assertThat(service.getReport(property).getStatus()).isEqualTo("REPORTED");
    assertThat(service.history(property)).hasSize(2);
    assertThatThrownBy(()->restarted.handleDamage(DamageAssessment.builder().propertyId(property).assessmentId(assessment)
        .damageAmount(BigDecimal.ZERO).assessedAt(d.getAssessedAt()).build())).isInstanceOf(IllegalArgumentException.class);
  }
  @Test void concurrentInstancesDoNotLoseDifferentBranches() throws Exception {
    var store=new MemoryReportStore();var one=new ReportingEventProcessor(new ReportingService(store));var two=new ReportingEventProcessor(new ReportingService(store));
    one.handleDamage(damage(Instant.now(),assessment));one.handleCompensation(compensation());
    try(var pool=Executors.newFixedThreadPool(2)) {
      var gate=new CountDownLatch(1);
      Future<?> a=pool.submit(()->{await(gate);one.handlePayment(payment());});
      Future<?> b=pool.submit(()->{await(gate);two.handleInvestment(investment());});gate.countDown();a.get();b.get();
    }
    assertThat(new ReportingService(store).getReport(property).getStatus()).isEqualTo("REPORTED");
  }
  @Test void allEventPermutationsReachTheSameAmounts() {
    List<Integer> values=new ArrayList<>(List.of(0,1,2,3));
    permute(values,0);
  }
  private void permute(List<Integer> list,int index) {
    if(index==list.size()) {
      var service=new ReportingService();var p=new ReportingEventProcessor(service);
      for(int event:list)switch(event){case 0->p.handleDamage(damage(Instant.now(),assessment));case 1->p.handleCompensation(compensation());case 2->p.handlePayment(payment());case 3->p.handleInvestment(investment());}
      assertThat(service.getReport(property).getStatus()).isEqualTo("REPORTED");
      assertThat(service.getReport(property).getTotalCompensation()).isEqualByComparingTo("10.01");
      return;
    }
    for(int i=index;i<list.size();i++){Collections.swap(list,index,i);permute(list,index+1);Collections.swap(list,index,i);}
  }
  private static void await(CountDownLatch gate){try{gate.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}}
}
