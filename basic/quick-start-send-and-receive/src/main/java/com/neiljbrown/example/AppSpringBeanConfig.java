/*
 * Copyright 2020-present the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neiljbrown.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * The root/parent Spring {@link Configuration} class for this application.
 * <p>
 * Imports all of the app's other {@link Configuration} classes to support the Spring ApplicationContext being
 * loaded from one location.
 * <p>
 * Declares any general-purpose Spring-managed beans that are commonly required or utilised to support the whole app.
 * <p>
 * Instructs  Spring to load application config from an external property file into the Spring Environment using a
 * {@link PropertySource}.
 */
@Configuration
@PropertySource("classpath:application.properties")
@Import(KafkaSpringBeanConfig.class)
public class AppSpringBeanConfig {

  /**
   * Register an instance of a Spring {@link PropertySourcesPlaceholderConfigurer} bean in order to enforce strict
   * checking of keys to application config values when injecting them into beans using the
   * {@link org.springframework.beans.factory.annotation.Value} annotation.
   *
   * @return the instance of {@link PropertySourcesPlaceholderConfigurer}.
   */
  @Bean
  // When configuring a PropertySourcesPlaceholderConfigurer using JavaConfig, the @Bean method must be static
  public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }
}