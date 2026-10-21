package com.gamecoach.datasource.overfast;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers OverFastProperties so Spring fills it from application.properties. */
@Configuration
@EnableConfigurationProperties(OverFastProperties.class)
public class OverFastConfig {
}
