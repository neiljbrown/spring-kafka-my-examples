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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.GenericMessage;

/*
 * Driver program for this sample app, which provides an example of how to use the following features of the Spring for
 * Kafka library/module.
 * <p>
 * <br>
 * <strong>1) KafkaTemplate</strong> - Using the {@link KafkaTemplate} class to simplify sending messages to a Kafka
 * topic, by creating and wrapping an instance of the Kafka client API's
 * {@link org.apache.kafka.clients.producer.KafkaProducer}, and providing a set
 * of overloaded convenience methods for sending.
 * <p>
 * <br>
 * <strong>2) POJO Message Listeners</strong> - How to take advantage of Spring for Kafka's
 * {@link KafkaMessageListenerContainer} support for simplifying the code that needs to be written to process
 * messages received on Kafka topics. The MessageListenerContainer (MLC) performs the 'heavy lifting' of maintaining a
 * reliable connection to the Kafka broker and consuming messages from one or more specified topics. Application code
 * only needs to provide a message handler method to which the MLC can dispatch messages, by registering a (POJO)
 * implementation of Spring for Kafka's {@link org.springframework.kafka.listener.MessageListener}.
 * <p>
 * <br>
 * <h2>Implementation Overview &amp; Runtime Dependencies</h2>
 * This driver program is notionally implemented as a JUnit test case for {@link QuickStartMessageListener} - a
 * sample implementation of Spring or Kafka's {@link org.springframework.kafka.listener.MessageListener}.
 * <p>
 * The test method(s) currently relies on connecting to a previously launched, locally running instance of the Kafka
 * broker, listening on the default port ({@link #KAFKA_BROKER_DEFAULT_PORT}). In practice, this test case would be
 * considered an integration test of the message listener with both the Spring framework and the real implementation
 * of the message broker.
 * <p>
 * For more details see logic in single test method {@link #tesOnMessage()}.
 */
public class QuickStartMessageListenerIntegrationTest {

  private static final String KAFKA_BROKER_DEFAULT_HOST = "localhost";
  private static final String KAFKA_BROKER_DEFAULT_PORT = "9092";
  private static final String KAFKA_TOPIC_1 = "topic1";
  private static final String KAFKA_TOPIC_2 = "topic2";

  private QuickStartMessageListener messageListener;
  private List<ConsumerRecord<Integer, String>> handledMessages = new ArrayList<>();
  private KafkaTemplate<Integer, String> kafkaTemplate;

  @BeforeEach
  void setUp() {
    this.messageListener = new QuickStartMessageListener(message -> this.handledMessages.add(message));
    this.kafkaTemplate = this.createKafkaTemplate(this.producerConfigProps());
  }

  /**
   * Integration test for {@link QuickStartMessageListener#onMessage(ConsumerRecord)}, which primarily serves to
   * illustrate how Spring for Kafka can be used on top of the Kafka client APIs to simplify sending and receiving
   * messages via Kafka, by both using a POJO based message listener, facilitated by Spring for Kafka's
   * {@link KafkaMessageListenerContainer}, to simplify consuming messages received on Kafka topics;  and Spring for
   * Kafka's {@link KafkaTemplate} to simplify sending messages to Kafka topics.
   *
   * @throws Exception if an unexpected error occurs on execution of this test.
   */
  @Test
  public void tesOnMessage() throws Exception {
    // Create an instance of the Spring KafkaMessageListener container that's configured to connect to Kafka,
    // consume from specific topics and dispatch received messages to our MessageListener
    ContainerProperties containerProperties = this.createMessageListenerContainerProperties();
    containerProperties.setMessageListener(this.messageListener);
    KafkaMessageListenerContainer<Integer, String> kafkaMessageListenerContainer =
      this.createKafkaMessageListenerContainer(containerProperties, this.consumerConfigProperties());
    kafkaMessageListenerContainer.start();
    Thread.sleep(1500); // Give it time to start

    // Configure our MessageListener with a latch to support this test blocking on it receiving all expected messages
    CountDownLatch countDownLatch = new CountDownLatch(4);
    this.messageListener.setProcessedMessageLatch(countDownLatch);

    // *** Example Usage of KafkaTemplate to send messages

    // KafkaTemplate can be configured to send messages to a default topic
    this.kafkaTemplate.setDefaultTopic(KAFKA_TOPIC_1);

    // Send a message to the default topic, without a partition, comprising the specified key and value.
    this.kafkaTemplate.sendDefault(1,"foo");

    // Send a message to a specific topic AND partition, comprising the specified key and value.
    this.kafkaTemplate.send(KAFKA_TOPIC_1, 0, 2, "bar");

    // Send a message with domain-specific headers using Kafka client API's ProducerRecord class
    ProducerRecord<Integer, String> producerRecord = new ProducerRecord<>(KAFKA_TOPIC_1, null, 3, "baz");
    producerRecord.headers().add("my-event-id", "1".getBytes());
    producerRecord.headers().add("my-event-type", "user".getBytes());
    kafkaTemplate.send(producerRecord);

    // Send a message with domain-specific headers using Spring Message interface. The Kafka record (message) value is
    // sourced from the Message's payload. Other values supported when sending a Kafka message, as defined in the
    // Kafka client API's ProducerRecord class, must be supplied as Message headers using provided Spring constants.
    kafkaTemplate.send(new GenericMessage<>("fudge", Map.of(
        KafkaHeaders.TOPIC, KAFKA_TOPIC_1, // KafkaTemplate's default topic used if not provided. One or other required.
        KafkaHeaders.PARTITION_ID, 0, // Optional
        KafkaHeaders.MESSAGE_KEY, 4, // Optional
        // Note - Additional domain-specific headers are only included in the Kafka message if value of type byte[]
        "my-event-id", "2".getBytes(StandardCharsets.UTF_8),
        "my-event-type", "user".getBytes(StandardCharsets.UTF_8)
      ))
    );

    assertThat(countDownLatch.await(5, TimeUnit.SECONDS))
      .withFailMessage("Timed-out waiting for MessageListener under test to receive all sent messages.")
      .isTrue();

    // Assert details of the last received message
    assertThat(this.handledMessages.get(3).value()).isEqualTo("fudge");
    assertThat(this.handledMessages.get(3).key()).isEqualTo(4);
    // Kafka ConsumerRecord header values are binary (raw byte[]) and have to be converted back to expected type
    Header myEventIdHeader = this.handledMessages.get(3).headers().lastHeader("my-event-id");
    assertThat(myEventIdHeader).isNotNull();
    assertThat(new String(myEventIdHeader.value(),StandardCharsets.UTF_8)).isEqualTo("2");

    kafkaMessageListenerContainer.stop();
  }

  /**
   * Creates the instance of Spring for Kafka {@link KafkaTemplate} that this app uses to simplify publishing/sending
   * messages to a Kafka topic.
   *
   * @param producerConfigProps  a set of properties should be used to configure the underlying Kafka client API's
   * {@link org.apache.kafka.clients.producer.KafkaProducer} that the created KafkaTemplate uses, in the form of a Map
   * of supported {@link org.apache.kafka.clients.producer.ProducerConfig} key and value pairs.
   *
   * @return the created {@link KafkaTemplate}.
   */
  private KafkaTemplate<Integer, String> createKafkaTemplate(Map<String,Object> producerConfigProps) {
    ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(producerConfigProps);
    return new KafkaTemplate<>(pf);
  }

  /**
   * Creates and returns the instance of Spring for Kafka {@link KafkaMessageListenerContainer} that this app
   * uses to integrate with Kafka and dispatch messages to registered POJO
   * {@link org.springframework.kafka.listener.MessageListener}.
   *
   * @param containerProps the {@link ContainerProperties} to use to configured the listener container.
   * @return the created {@link KafkaMessageListenerContainer}.
   *
   * @param consumerConfigProps a set of properties that the ConsumerFactory should use to configure the Kafka
   * Consumer instances that it creates, in the form of a Map of supported {@link ConsumerConfig} key and value pairs.
   */
  private KafkaMessageListenerContainer<Integer, String> createKafkaMessageListenerContainer(
    ContainerProperties containerProps, Map<String,Object> consumerConfigProps) {
    // Create a Spring for Kafka ConsumerFactory for the listener container to use to create its Kafka Consumer(s)
    DefaultKafkaConsumerFactory<Integer, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerConfigProps);
    return new KafkaMessageListenerContainer<>(consumerFactory, containerProps);
  }

  /**
   * Creates a set of properties (key and value) that should be used to configure created Kafka Producers.
   * <p>
   * These properties ultimately (via a Spring for Kafka {@link org.springframework.kafka.core.ProducerFactory} get
   * used to create the Kafka client API's {@link org.apache.kafka.clients.producer.KafkaProducer}. The property keys
   * are supported {@link ProducerConfig}.
   *
   * @return a {@link Map} containing the properties.
   */
  private Map<String, Object> producerConfigProps() {
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
   * Creates  a set of properties (configuration) to be used when creating the Spring for Kafka
   * {@link KafkaMessageListenerContainer}.
   *
   * @return the created  {@link ContainerProperties}.
   */
  private ContainerProperties createMessageListenerContainerProperties() {
    // Include the name of topics to which the MessageListenerContainer should subscribe
    return new ContainerProperties(KAFKA_TOPIC_1, KAFKA_TOPIC_2);
  }

  /**
   * Creates a set of properties (key and value) that should be used to configure created Kafka Consumers.
   * <p>
   * These properties ultimately (via a Spring for Kafka {@link org.springframework.kafka.core.ConsumerFactory} get
   * used to create the Kafka client API's {@link org.apache.kafka.clients.consumer.KafkaConsumer}. The property keys
   * are supported {@link ConsumerConfig}.
   *
   * @return a {@link Map} containing the properties.
   */
  private Map<String, Object> consumerConfigProperties() {
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