package com.smartpark.swp391.infrastructure.demo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DemoSeedProperties.class)
public class DemoSeedConfig {}
