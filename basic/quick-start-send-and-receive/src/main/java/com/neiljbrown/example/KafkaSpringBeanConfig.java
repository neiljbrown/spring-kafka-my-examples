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

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.MessageListenerContainer;

/**
 * A Spring {@code @Configuration} class that declares the Spring-managed beans that this app uses to support sending
 * and receiving messages via its Kafka message broker, with the help of the Spring for Kafka project. See the @Bean
 * annotated methods for details.
 * <br><br>
 * <h2>Use of EnableKafka Class Annotation</h2>
 * The class uses the Spring for Kafka project's {@link EnableKafka} annotation as a convenient way to automate
 * enabling support for POJO-based message listeners for processing messages received on specified Kafka topic(s).
 * The annotation support includes creating and registering a default set of supporting infrastructure beans
 * e.g. a KafkaMessageListenerContainer, and detecting message handler methods declared using the @KafkaListener
 * annotation on any registered Spring beans (removing the need for classes to implement Spring for Kafka's
 * {@link org.springframework.kafka.listener.MessageListener} interface. For a Spring-based Kafka messaging app the
 * EnableKafka annotation serves a similar purpose to the EnableWebMvc annotation for a Spring MVC app.
 */
@Configuration
@EnableKafka
public class KafkaSpringBeanConfig {

  private static final String KAFKA_BROKER_DEFAULT_HOST = "localhost";
  private static final String KAFKA_BROKER_DEFAULT_PORT = "9092";

  /**
   * Creates the instance of a Spring for Kafka {@link KafkaTemplate} that this app uses to simplify publishing/sending
   * messages to a Kafka topic.
   *
   * @return the created {@link KafkaTemplate}.
   */
  @Bean
  KafkaTemplate<Integer, String> kafkaTemplate() {
    ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(createProducerConfigProps());
    return new KafkaTemplate<>(pf);
  }

  /**
   * Creates an instance of a POJO-based message listener containing message handler methods (business logic) for
   * processing messages received on Kafka topic(s). The message handler methods are declared using the @KafkaListener
   * annotation to support their registration with the Spring for Kafka KafkaMessageListenerContainer, which handles
   * consuming messages from the Kafka topic and dispatching them to the relevant handler method(s).
   *
   * @return the created {@link QuickStartMessageListener}.
   */
  @Bean
  QuickStartMessageListener quickStartMessageListener() {
    return new QuickStartMessageListener(null);
  }

  /**
   * Creates and returns the instance of Spring for Kafka
   * {@link org.springframework.kafka.config.KafkaListenerContainerFactory} that @EnableKafka infrastructure should
   * use to create the Kafka {@link MessageListenerContainer} that's used to support app's @KafkaListener message
   * handlers.
   *
   * @return the created {@link ConcurrentKafkaListenerContainerFactory}.
   */
  @Bean
  ConcurrentKafkaListenerContainerFactory<Integer, String> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<Integer, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
  }

  /**
   * Creates and returns a Spring for Kafka {@link ConsumerFactory} that the KafkaListenerContainer should use to create
   * its Kafka Consumer(s).
   * <p>
   * The created factory is configured with a set of properties that should be used to configure the Kafka Consumer
   * it creates. See {@link #createConsumerConfigProperties()}.
   *
   * @return the created {@link ConsumerFactory}.
   */
  @Bean
  ConsumerFactory<Integer, String> consumerFactory() {
    return new DefaultKafkaConsumerFactory<>(createConsumerConfigProperties());
  }

  /**
   * Creates a set of properties (key and value) that should be used to configure created instances of KafkaProducer
   * used by the app's {@link KafkaTemplate}.
   * <p>
   * These properties ultimately (via a Spring for Kafka {@link org.springframework.kafka.core.ProducerFactory} get
   * used to create the Kafka client API's {@link org.apache.kafka.clients.producer.KafkaProducer}. The property keys
   * are supported {@link ProducerConfig}.
   *
   * @return a {@link Map} containing the properties.
   * @see #kafkaTemplate()
   */
  private Map<String, Object> createProducerConfigProps() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER_DEFAULT_HOST+":"+ KAFKA_BROKER_DEFAULT_PORT);
    props.put(ProducerConfig.RETRIES_CONFIG, 0);
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
    props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return props;
  }

  /**
   * Creates a set of properties (key and value) that should be used to configure created instances of KafkaConsumer
   * used by the app's {@link ConsumerFactory}.
   * <p>
   * These properties ultimately (via a Spring for Kafka {@link org.springframework.kafka.core.ConsumerFactory} get
   * used to create the Kafka client API's {@link org.apache.kafka.clients.consumer.KafkaConsumer}. The property keys
   * are supported {@link ConsumerConfig}.
   *
   * @return a {@link Map} containing the properties.
   */
  private Map<String, Object> createConsumerConfigProperties() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER_DEFAULT_HOST+":"+ KAFKA_BROKER_DEFAULT_PORT);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "springKafkaQuickStartGroup");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return props;
  }
}