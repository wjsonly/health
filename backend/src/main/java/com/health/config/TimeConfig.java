package com.health.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {
    @Bean
    Clock businessClock(@Value("${health.business-zone:Asia/Shanghai}") String businessZone) {
        return Clock.system(ZoneId.of(businessZone));
    }
}
