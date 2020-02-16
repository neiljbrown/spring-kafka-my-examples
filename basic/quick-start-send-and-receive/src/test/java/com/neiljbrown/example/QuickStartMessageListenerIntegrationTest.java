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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/*
 * Driver program for this sample app implemented in the form of Spring container integration test for the {@link
 * QuickStartMessageListener}. Provides an example of how to use the following features of the Spring for Kafka
 * library/module.
 * <p>
 * <br>
 * <strong>1) KafkaTemplate</strong> - Using the {@link KafkaTemplate} class to simplify sending messages to a Kafka
 * topic, by creating and wrapping an instance of the Kafka client API's
 * {@link org.apache.kafka.clients.producer.KafkaProducer}, and providing a set of overloaded convenience methods for
 * sending.
 * <p>
 * <br>
 * <strong>2) POJO Message Listeners</strong> - How to take advantage of Spring for Kafka's
 * {@link KafkaMessageListenerContainer} support for simplifying the code that needs to be written to process
 * messages received on Kafka topics. The MessageListenerContainer (MLC) performs the 'heavy lifting' of maintaining a
 * reliable connection to the Kafka broker, consuming messages from one or more specified topics, and acknowledging
 * them when successfully process by the app. Application code only needs to provide a message handler method
 * (containing the business logic) to which the MLC can dispatch messages, by registering a (POJO) implementation of
 * Spring for Kafka's {@link org.springframework.kafka.listener.MessageListener}, either directly or by declaring
 * handler methods using the {@link KafkaListener} annotation.
 * <p>
 * <br>
 * <h2>Implementation Overview &amp; Runtime Dependencies</h2>
 * This driver program is notionally implemented as a JUnit test case for {@link QuickStartMessageListener} - a
 * sample implementation of Spring or Kafka's {@link org.springframework.kafka.listener.MessageListener}.
 * <p>
 * The test method(s) currently relies on connecting to a previously launched, locally running instance of the Kafka
 * broker, listening on the default port (see {@link KafkaSpringBeanConfig#KAFKA_BROKER_DEFAULT_PORT}). In practice,
 * this test case would be considered an integration test of the message listener with both the Spring framework and
 * the real implementation of the message broker.
 * <p>
 * For more details see logic in single test method {@link #tesOnMessage()}.
 */
// Instruct JUnit 5 to extend the test with Spring support as provided by the Spring TestContext framework; And
// instruct the TestContext framework to load a Spring AppContext from the specified bean config class
@SpringJUnitConfig(KafkaSpringBeanConfig.class)
public class QuickStartMessageListenerIntegrationTest {

  private static final String MSG_HDR_MY_EVENT_ID = "my-event-id";
  private static final String MSG_HDR_MY_EVENT_TYPE = "my-event-type";

  private QuickStartMessageListener messageListener;
  private KafkaTemplate<Integer, String> kafkaTemplate;
  private List<ConsumerRecord<Integer, String>> handledMessages = new ArrayList<>();

  /**
   * Creates an instance of this test from its supplied dependencies.
   *
   * @param messageListener the instance of the {@link QuickStartMessageListener} under test. This should be a Spring
   * managed bean whose @KafkaListener annotated message handler methods have detected by and registered with Spring
   * for Kafka's MessageListenerContainer.
   * @param kafkaTemplate a {@link KafkaTemplate} capable of sending messages with an Integer key and a String value.
   */
  @Autowired
  public QuickStartMessageListenerIntegrationTest(QuickStartMessageListener messageListener,
    KafkaTemplate<Integer, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
    this.messageListener = messageListener;
  }

  @BeforeEach
  void setUp() {
    // Capture the messages processed by the class under test to support asserting receipt & processing
    this.messageListener.setMessageProcessor(message -> this.handledMessages.add(message));
  }

  /**
   * Integration test for {@link QuickStartMessageListener#onMessage(ConsumerRecord)}, which primarily serves to
   * illustrate how Spring for Kafka can be used on top of the Kafka client APIs to simplify sending and receiving
   * messages via Kafka, by both using a POJO based message listener, facilitated by Spring for Kafka's
   * {@link KafkaMessageListenerContainer}, to simplify consuming messages received on Kafka topics; and Spring for
   * Kafka's {@link KafkaTemplate} to simplify sending messages to Kafka topics.
   *
   * @throws Exception if an unexpected error occurs on execution of this test.
   */
  @Test
  public void tesOnMessage() throws Exception {
    // Configure our MessageListener with a latch to support this test blocking on it receiving all expected messages
    CountDownLatch countDownLatch = new CountDownLatch(4);
    this.messageListener.setProcessedMessageLatch(countDownLatch);

    // *** Example Usage of KafkaTemplate to send messages

    // KafkaTemplate can be configured to send messages to a default topic
    this.kafkaTemplate.setDefaultTopic(QuickStartMessageListener.KAFKA_TOPIC_USER_EVENTS);

    // Send a message to the default topic, without a partition, comprising the specified key and value.
    this.kafkaTemplate.sendDefault(1,"{\"userId\": 1, \"firstName\": \"joe\"}");

    // Send a message to a specific topic AND partition, comprising the specified key and value.
    this.kafkaTemplate.send(QuickStartMessageListener.KAFKA_TOPIC_USER_EVENTS, 0, 2, "{\"userId\": 2, \"firstName\": \"jane\"}");

    // Send a message with domain-specific headers using Kafka client API's ProducerRecord class
    ProducerRecord<Integer, String> producerRecord = new ProducerRecord<>(QuickStartMessageListener.KAFKA_TOPIC_USER_EVENTS, null, 3,
      "{\"userId\": 3, \"firstName\": \"jack\"}");
    producerRecord.headers().add(MSG_HDR_MY_EVENT_ID, "123".getBytes());
    producerRecord.headers().add(MSG_HDR_MY_EVENT_TYPE, "UserCreated".getBytes());
    this.kafkaTemplate.send(producerRecord);

    // Send a message with domain-specific headers using Spring Message interface. The Kafka record (message) value is
    // sourced from the Message's payload. Other values supported when sending a Kafka message, as defined in the
    // Kafka client API's ProducerRecord class, must be supplied as Message headers using provided Spring constants.
    final String userCreatedEventJson = "{\"userId\": 4, \"firstName\": \"jim\"}";
    final String myEventId = "124";
    final GenericMessage<String> eventMessage = new GenericMessage<>(userCreatedEventJson, Map.of(
      KafkaHeaders.TOPIC, QuickStartMessageListener.KAFKA_TOPIC_USER_EVENTS, // KafkaTemplate's default topic used if not provided. One or other required.
      KafkaHeaders.PARTITION_ID, 0, // Optional
      KafkaHeaders.MESSAGE_KEY, 4, // Optional
      // Note - Additional domain-specific headers are only included in the Kafka message if value of type byte[]
      MSG_HDR_MY_EVENT_ID, myEventId.getBytes(StandardCharsets.UTF_8),
      MSG_HDR_MY_EVENT_TYPE, "UserCreated".getBytes(StandardCharsets.UTF_8)
    ));
    this.kafkaTemplate.send(eventMessage);

    assertThat(countDownLatch.await(5, TimeUnit.SECONDS))
      .withFailMessage("Timed-out waiting for MessageListener under test to receive all sent messages.")
      .isTrue();

    // Assert details of the last received message
    assertThat(this.handledMessages.get(3).value()).isEqualTo(userCreatedEventJson);
    assertThat(this.handledMessages.get(3).key()).isEqualTo(eventMessage.getHeaders().get(KafkaHeaders.MESSAGE_KEY));
    // Kafka ConsumerRecord header values are binary (raw byte[]) and have to be converted back to expected type
    Header myEventIdHeader = this.handledMessages.get(3).headers().lastHeader(MSG_HDR_MY_EVENT_ID);
    assertThat(myEventIdHeader).isNotNull();
    assertThat(new String(myEventIdHeader.value(),StandardCharsets.UTF_8)).isEqualTo(myEventId);
  }
}