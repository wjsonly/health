package com.health.wechat;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MiniProgramAuthWebConfig implements WebMvcConfigurer {
    private final MiniProgramAuthInterceptor miniProgramAuthInterceptor;

    public MiniProgramAuthWebConfig(MiniProgramAuthInterceptor miniProgramAuthInterceptor) {
        this.miniProgramAuthInterceptor = miniProgramAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(miniProgramAuthInterceptor)
                .addPathPatterns("/api/appointments", "/api/appointments/**");
    }
}
