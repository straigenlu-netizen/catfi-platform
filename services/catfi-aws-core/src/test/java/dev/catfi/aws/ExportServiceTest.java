package dev.catfi.aws;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ExportServiceTest {
  static class Jobs implements JobRepository {
    final Map<String,ExportJob> data=new HashMap<>();
    public Optional<ExportJob> find(String id){return Optional.ofNullable(data.get(id));}
    public ExportJob create(ExportJob j){data.putIfAbsent(j.jobId(),j);return data.get(j.jobId());}
    public void status(String id,String s,String key,String error){var j=data.get(id);data.put(id,new ExportJob(id,j.propertyId(),j.ownerSub(),j.reportJson(),s,key,j.createdAt(),error));}
  }
  @Test void retriesTheSameFrozenFileAfterCrashBeforeCompletion() {
    Jobs jobs=new Jobs();jobs.create(new ExportJob("job","property","owner","{\"amount\":10.01}","PENDING","","now",""));
    Map<String,String> files=new HashMap<>();ExportService service=new ExportService(jobs,(key,json,type)->files.put(key,json));
    String key=service.generate("job"); // Simulated process death before complete.
    assertThat(jobs.find("job").orElseThrow().status()).isEqualTo("RUNNING");
    String recovered=new ExportService(jobs,(k,json,type)->files.put(k,json)).generate("job");
    assertThat(recovered).isEqualTo(key);assertThat(files).hasSize(1).containsValue("{\"amount\":10.01}");
    service.complete("job",key);assertThat(service.generate("job")).isEqualTo(key);
    assertThat(jobs.find("job").orElseThrow().status()).isEqualTo("SUCCEEDED");
  }
  @Test void refusesForgedOutputKey() {
    Jobs jobs=new Jobs();jobs.create(new ExportJob("job","property","owner","{}","RUNNING","","now",""));
    assertThatThrownBy(()->new ExportService(jobs,(k,c,t)->{}).complete("job","other-users-file")).isInstanceOf(IllegalArgumentException.class);
  }
  @Test void storesPendingJobsAndRejectsWrongOwnerAndIdempotencyReuse() {
    Jobs jobs=new Jobs();MemoryReportStore reports=new MemoryReportStore();String p=UUID.randomUUID().toString();
    reports.compareAndSet(new ReportDocument(p,1,"{}","{\"exact\":10.01}","REPORTED","assessment","now"),0);
    ExportRequests requests=new ExportRequests(jobs,reports,(sub,id)->sub.equals("owner"));
    ExportJob job=requests.request("owner",p,"key_123456");
    assertThat(requests.request("owner",p,"key_123456").jobId()).isEqualTo(job.jobId());
    assertThatThrownBy(()->requests.get("another",job.jobId())).isInstanceOf(SecurityException.class);
    assertThatThrownBy(()->requests.request("owner",UUID.randomUUID().toString(),"key_123456")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(()->requests.request("owner",p,"short")).isInstanceOf(IllegalArgumentException.class);
  }
}
