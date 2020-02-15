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

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * An example of a POJO message listener that contains message handler methods capable of processing notional User
 * domain events that are dispatched to it.
 * <p>
 * This message listener class supports working in conjunction with a Spring for Kafka
 * {@link org.springframework.kafka.listener.KafkaMessageListenerContainer} (MLC). The MLC does the heavy lifting of
 * maintaining a reliable connection to the message broker, consuming the messages from Kafka topic(s), dispatching
 * them to this listener, and acknowledging them when they're successfully processed.
 * <p>
 * After having applied the {@link org.springframework.kafka.annotation.EnableKafka} annotation in the application's
 * Spring bean config, this class no longer needs to implement the Spring for Kafka
 * {@link org.springframework.kafka.listener.MessageListener} interface nor register itself with the MLC. Instead,
 * this listener's available message handler methods can be made discoverable by declaring them using the
 * {@link KafkaListener} annotation. This annotation is also used to specify the topic(s) from which handler method
 * listens and consumes messages.
 */
public class QuickStartMessageListener {

  /** The name of the Kafka topic from which User domain events are consumed. */
  static final String KAFKA_TOPIC_USER_EVENTS = "user-events";

  private static final Logger logger = LoggerFactory.getLogger(QuickStartMessageListener.class);

  private Consumer<ConsumerRecord<Integer, String>> messageProcessor;
  private CountDownLatch latch = new CountDownLatch(0);

  /**
   * @param messageProcessor an optional instance of {@link Consumer} that should be invoked to process new
   * messages received by {@link #onMessage(ConsumerRecord)}. This arg is purely provided for testing purposes.
   * (Processing would typically be done by {@link #onMessage(ConsumerRecord)} itself). MessageListener can't return
   * values, so this argument supports test code verifying each received message.
   */
  public QuickStartMessageListener(Consumer<ConsumerRecord<Integer, String>> messageProcessor) {
    if(messageProcessor != null) {
      this.messageProcessor = messageProcessor;
    }
  }

  /**
   * This sample implementation pseudo processes a received Kafka message by decrementing the instance's CountDownLatch
   * (supporting test code coordinating/blocking on no,. of messages processed), and passing the message to the
   * instance's {@link Consumer} if one was supplied on construction.
   *
   * @param message the Kafka message to be processed in the form of a {@link ConsumerRecord}.
   * @see #setProcessedMessageLatch(CountDownLatch)
   */
  @KafkaListener(topics = KAFKA_TOPIC_USER_EVENTS)
  public void onMessage(ConsumerRecord<Integer, String> message) {
    logger.debug("Received message [" + message + "].");
    this.latch.countDown();
    if (this.messageProcessor !=null) {
      this.messageProcessor.accept(message);
    }
  }

  /**
   * Sets the instance of the {@link CountDownLatch} that this message listener should use for its
   * {@link #latch processedMessageLatch} property.
   * <p>
   * This property is only used to support testing of this class. The message listener decrements the latch each time it
   * processes a new message using one of its message handler methods, e.g. {@link #onMessage(ConsumerRecord)}. This
   * supports collaborating (test) code blocking until this listener has received and processed an expected no. of
   * messages.
   * 
   * @param latch the {@link CountDownLatch}.
   */
  public void setProcessedMessageLatch(CountDownLatch latch) {
    Objects.requireNonNull(latch, "latch must not be null.");
    this.latch = latch;
  }

  /**
   * Sets the instance of a {@link Consumer} functional interface that this message listener should  use for its
   * {@link #messageProcessor} property.
   * <p>
   * This property is only provided to support testing of this class. (The {@link #onMessage(ConsumerRecord)} method
   * would typically fully process the message itself). A message handler methods that is based on implementing
   * {@link org.springframework.kafka.listener.MessageListener#onMessage(Object)} can't return values. Setting this
   * property supports test code verifying each received message.
   *
   * @param messageProcessor the {@link Consumer}.
   */
  public void setMessageProcessor(Consumer<ConsumerRecord<Integer, String>> messageProcessor) {
    this.messageProcessor = messageProcessor;
  }
}