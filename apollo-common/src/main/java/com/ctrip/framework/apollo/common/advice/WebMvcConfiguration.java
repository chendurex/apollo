package com.ctrip.framework.apollo.common.advice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author cheny.huang
 * @date 2018-08-23 17:23.
 */
@Configuration
public class WebMvcConfiguration {
    @Bean
    public WebMvcConfigurerAdapter configurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new CustomizePathLogInterceptor()).addPathPatterns("/**");
            }
        };
    }

}
