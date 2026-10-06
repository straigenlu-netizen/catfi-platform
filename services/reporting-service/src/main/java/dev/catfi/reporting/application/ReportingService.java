package dev.catfi.reporting.application;

import dev.catfi.aws.*;
import dev.catfi.common.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportingService {
  private final ReportStore store;
  public ReportingService() { this(new MemoryReportStore()); }
  @Autowired public ReportingService(ReportStore store) { this.store=store; }

  public void accept(UUID property, String type, String id, Object event) {
    Objects.requireNonNull(property,"propertyId");
    String eventJson=Json.write(event); String receipt=type+":"+id;
    for(int attempt=0;attempt<12;attempt++) {
      var old=store.find(property.toString()); long version=old.map(ReportDocument::version).orElse(0L);
      ReportState state=old.map(v->Json.read(v.stateJson(),ReportState.class)).orElseGet(ReportState::new);
      String seen=state.receipts.get(receipt);
      if(seen!=null) {
        if(!seen.equals(eventJson))throw new IllegalArgumentException("Conflicting event payload: "+receipt);
        return;
      }
      if(state.receipts.size()>=512)throw new IllegalStateException("Property retention limit reached; archive before more events");
      state.receipts.put(receipt,eventJson);
      if(event instanceof DamageAssessment d) {
        state.damage.put(d.getAssessmentId().toString(),d);
        DamageAssessment previous=state.damage.get(state.latestAssessment);
        if(previous==null||isNewer(d,previous))state.latestAssessment=d.getAssessmentId().toString();
      } else if(event instanceof CompensationResult c) {
        putUnique(state.compensation,c.getDamageAssessmentId().toString(),c);
      } else if(event instanceof Payment p) {
        putUnique(state.payment,p.getCompensationResultId().toString(),p);
      } else if(event instanceof InvestmentDecision i) {
        putUnique(state.investment,i.getCompensationResultId().toString(),i);
      } else throw new IllegalArgumentException("Unsupported reporting event");
      PropertyFullReport report=project(property,state);
      String status=report==null?"PENDING":report.getStatus();
      var next=new ReportDocument(property.toString(),version+1,Json.write(state),report==null?"null":Json.write(report),
          status,state.latestAssessment==null?"":state.latestAssessment,Instant.now().toString());
      if(store.compareAndSet(next,version))return;
      try { Thread.sleep(Math.min(100,2L*(attempt+1)) + java.util.concurrent.ThreadLocalRandom.current().nextInt(10)); }
      catch(InterruptedException e) { Thread.currentThread().interrupt();throw new IllegalStateException("Interrupted",e); }
    }
    throw new IllegalStateException("Concurrent reporting update retry budget exhausted");
  }
  private static <T> void putUnique(Map<String,T> values,String key,T value) {
    T existing=values.get(key);
    if(existing!=null&&!Json.write(existing).equals(Json.write(value)))
      throw new IllegalArgumentException("Conflicting process result: "+key);
    values.put(key,value);
  }
  private boolean isNewer(DamageAssessment next,DamageAssessment previous) {
    // Legacy test/demo fixtures may lack timestamps. Real producer supplies assessedAt.
    if(next.getAssessedAt()==null||previous.getAssessedAt()==null)return true;
    int order=next.getAssessedAt().compareTo(previous.getAssessedAt());
    return order>0||(order==0&&next.getAssessmentId().compareTo(previous.getAssessmentId())>0);
  }
  private PropertyFullReport project(UUID property,ReportState state) {
    DamageAssessment d=state.damage.get(state.latestAssessment);if(d==null)return null;
    CompensationResult c=state.compensation.get(d.getAssessmentId().toString());
    Payment p=c==null?null:state.payment.get(c.getResultId().toString());
    InvestmentDecision i=c==null?null:state.investment.get(c.getResultId().toString());
    boolean paid=c!=null&&(c.getAmountToPay().signum()==0||p!=null);
    boolean invested=c!=null&&(c.getAmountToInvest().signum()==0||i!=null);
    return PropertyFullReport.builder().propertyId(property).totalDamage(d.getDamageAmount())
        .totalCompensation(c==null?BigDecimal.ZERO:c.getTotalCompensation()).actualPayout(p==null?BigDecimal.ZERO:p.getAmount())
        .currentPortfolio(i==null?Map.of():Map.copyOf(i.getAllocation())).reportGeneratedAt(Instant.now())
        .status(paid&&invested?"REPORTED":"IMPACTED").build();
  }
  public PropertyFullReport getReport(UUID property) {
    return store.find(property.toString()).map(v->Json.read(v.reportJson(),PropertyFullReport.class)).orElse(null);
  }
  public Map<UUID,PropertyFullReport> getAllReports() {
    Map<UUID,PropertyFullReport> result=new LinkedHashMap<>();String cursor=null;
    do {
      ReportStore.Page page=store.list(cursor,100);
      for(var d:page.items()) { var report=Json.read(d.reportJson(),PropertyFullReport.class);if(report!=null)result.put(report.getPropertyId(),report); }
      cursor=page.cursor();
    } while(cursor!=null);
    return Map.copyOf(result);
  }
  public ReportStore.Page page(String cursor,int limit) { return store.list(cursor,limit); }
  public List<PropertyFullReport> history(UUID property) {
    return store.find(property.toString()).map(v->{
      ReportState state=Json.read(v.stateJson(),ReportState.class);String latest=state.latestAssessment;
      return state.damage.keySet().stream().map(id->{state.latestAssessment=id;return project(property,state);}).toList();
    }).orElse(List.of());
  }
  // Compatibility seam for existing component tests and consumers; does not replace correlation processing.
  public void updateReport(UUID p,BigDecimal d,BigDecimal c,BigDecimal paid,Map<String,BigDecimal> portfolio) {
    updateReport(p,d,c,paid,portfolio,"REPORTED");
  }
  public void updateReport(UUID p,BigDecimal d,BigDecimal c,BigDecimal paid,Map<String,BigDecimal> portfolio,String status) {
    for(int attempt=0;attempt<12;attempt++) {
      var old=store.find(p.toString());long version=old.map(ReportDocument::version).orElse(0L);
      var report=PropertyFullReport.builder().propertyId(p).totalDamage(d).totalCompensation(c).actualPayout(paid)
          .currentPortfolio(Map.copyOf(portfolio)).reportGeneratedAt(Instant.now()).status(status).build();
      if(store.compareAndSet(new ReportDocument(p.toString(),version+1,old.map(ReportDocument::stateJson).orElse(Json.write(new ReportState())),
          Json.write(report),status,old.map(ReportDocument::assessmentId).orElse(""),Instant.now().toString()),version))return;
    }
    throw new IllegalStateException("Concurrent update retry budget exhausted");
  }
}
