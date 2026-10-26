package com.gamecoach.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Puts the daily upload cap in front of both upload endpoints. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UploadQuota quota;

    public WebConfig(UploadQuota quota) {
        this.quota = quota;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(quota).addPathPatterns("/api/rivals/matches", "/api/overwatch/matches");
    }
}
