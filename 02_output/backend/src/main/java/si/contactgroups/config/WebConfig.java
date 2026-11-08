package si.contactgroups.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import si.contactgroups.service.GroupAccessService;
import si.contactgroups.web.GroupAccessInterceptor;

/** Registers the authorisation check of every Group endpoint (ADR-05). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final GroupAccessService access;

  public WebConfig(GroupAccessService access) {
    this.access = access;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new GroupAccessInterceptor(access));
  }
}
