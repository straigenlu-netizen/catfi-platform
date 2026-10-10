package dev.catfi.weather.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Enable the runtime cache without forcing controller slice tests to create a cache provider. */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfiguration {}
