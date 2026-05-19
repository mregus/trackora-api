package com.fleetwise.api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        var reg = new FilterRegistrationBean<RequestIdFilter>();
        reg.setFilter(new RequestIdFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        return reg;
    }
}
