package dev.catfi.notification;
import java.util.UUID;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component @ConditionalOnProperty(name="catfi.notification.mail-mode",havingValue="log",matchIfMissing=true)
public class LogMailSender implements MailSender {
  private static final Logger log=LoggerFactory.getLogger(LogMailSender.class);
  public String send(String recipient,String subject,String body){
    String id="local-"+UUID.randomUUID();
    log.info("Notification accepted recipientDomain={} subject={} messageId={}",domain(recipient),subject,id);
    return id;
  }
  private String domain(String email){int i=email.indexOf('@');return i<0?"invalid":email.substring(i+1);}
}
