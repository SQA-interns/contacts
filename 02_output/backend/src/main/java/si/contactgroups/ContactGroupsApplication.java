package si.contactgroups;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point of the Contact Groups backend. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ContactGroupsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ContactGroupsApplication.class, args);
  }
}
