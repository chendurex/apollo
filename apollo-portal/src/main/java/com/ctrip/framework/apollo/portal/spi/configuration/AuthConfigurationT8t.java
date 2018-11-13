package com.ctrip.framework.apollo.portal.spi.configuration;

import com.ctrip.framework.apollo.portal.spi.LogoutHandler;
import com.ctrip.framework.apollo.portal.spi.SsoHeartbeatHandler;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.spi.defaultimpl.DefaultLogoutHandler;
import com.ctrip.framework.apollo.portal.spi.defaultimpl.DefaultSsoHeartbeatHandler;
import com.ctrip.framework.apollo.portal.spi.t8t.T8tAuthenticationProvider;
import com.ctrip.framework.apollo.portal.spi.t8t.T8tUserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.t8t.T8tUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;


@Configuration
public class AuthConfigurationT8t {

  /**
   * spring.profiles.active = t8t
   */
  @Configuration
  @Profile({"t8t"})
  static class T8tAuthAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(UserInfoHolder.class)
    public UserInfoHolder t8tUserInfoHolder() {
      return new T8tUserInfoHolder();
    }
    @Bean
    @ConditionalOnMissingBean(UserService.class)
    public UserService t8tUserService() {
      return new T8tUserService();
    }

    /*copy from default*/
    @Bean
    @ConditionalOnMissingBean(SsoHeartbeatHandler.class)
    public SsoHeartbeatHandler defaultSsoHeartbeatHandler() {
      return new DefaultSsoHeartbeatHandler();
    }

    @Bean
    @ConditionalOnMissingBean(LogoutHandler.class)
    public DefaultLogoutHandler logoutHandler() {
      return new DefaultLogoutHandler();
    }
  }

  @Order(99)
  @Profile("t8t")
  @Configuration
  @EnableWebSecurity
  @EnableGlobalMethodSecurity(prePostEnabled = true)
  static class SpringSecurityConfigurer extends WebSecurityConfigurerAdapter {

    public static final String USER_ROLE = "user";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.csrf().disable();
      http.headers().frameOptions().sameOrigin();
      http.authorizeRequests()
              .antMatchers("/openapi/*").permitAll()
              .antMatchers("/*").hasAnyRole(USER_ROLE);
      http.formLogin().loginPage("/signin").permitAll().failureUrl("/signin?#/error").and().httpBasic();
      http.logout().invalidateHttpSession(true).clearAuthentication(true).logoutSuccessUrl("/signin?#/logout");
      http.exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/signin"));
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        return new T8tAuthenticationProvider();
    }

      @Override
      protected void configure(AuthenticationManagerBuilder auth) throws Exception {
          auth.authenticationProvider(authenticationProvider())
                  .eraseCredentials(true);
                  // .userDetailsService(loginService());
                  // .passwordEncoder(passwordEncoder());
      }
  }
}
