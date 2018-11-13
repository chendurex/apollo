package com.ctrip.framework.apollo.portal.spi.t8t;

import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.ctrip.filters.UserAccessFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("t8t")
public class WebContextConfiguration {
  @Autowired
  private UserInfoHolder userInfoHolder;

  @Bean
  public FilterRegistrationBean userAccessFilter() {
    FilterRegistrationBean filter = new FilterRegistrationBean();
    filter.setFilter(new UserAccessFilter(userInfoHolder));
    filter.addUrlPatterns("/*");
    return filter;
  }

}
