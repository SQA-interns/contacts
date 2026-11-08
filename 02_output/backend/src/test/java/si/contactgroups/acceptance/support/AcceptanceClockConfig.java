package si.contactgroups.acceptance.support;

import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Puts the {@link MutableClock} in place of the product's {@link Clock} bean (ADR-09). */
@TestConfiguration(proxyBeanMethods = false)
public class AcceptanceClockConfig {

  @Bean
  @Primary
  public MutableClock acceptanceTestClock() {
    return new MutableClock();
  }
}
