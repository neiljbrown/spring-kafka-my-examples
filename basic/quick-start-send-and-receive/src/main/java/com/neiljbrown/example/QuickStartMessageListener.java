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
import org.springframework.kafka.listener.MessageListener;

/**
 * A POJO implementation of a Spring for Kafka {@link MessageListener} that can be used to process individual Kafka
 * messages dispatched to it by a Spring for Kafka
 * {@link org.springframework.kafka.listener.KafkaMessageListenerContainer}, which does the heavy lifting of
 * maintaining a reliable connection to the broker, consuming the messages from Kafka topic(s) and acknowledging them.
 * <p>
 * This is a quick-start sample implementation of a MessageListener so doesn't do much. For details of its behaviour
 * see {@link #onMessage(ConsumerRecord)}.
 * <p>
 * Note that the {@link MessageListener} interface is an {@link FunctionalInterface} so simple listeners could be
 * implemented anonymously using a lambda function.
 */
public class QuickStartMessageListener implements MessageListener<Integer, String> {

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
  @Override
  public void onMessage(ConsumerRecord<Integer, String> message) {
    logger.debug("Received message [" + message + "].");
    this.latch.countDown();
    if (this.messageProcessor !=null) {
      this.messageProcessor.accept(message);
    }
  }

  /**
   * Sets the instance of the {@link CountDownLatch} that this instance should count down (decrement) each time it
   * processes a new message via {@link #onMessage(ConsumerRecord)}. Supports collaborating (test) code blocking 
   * until this listener has received and processed an expected no. of messages in the latch.
   * 
   * @param latch the {@link CountDownLatch}.
   */
  public void setProcessedMessageLatch(CountDownLatch latch) {
    Objects.requireNonNull(latch, "latch must not be null.");
    this.latch = latch;
  }

  /**
   * @return this instance's {@link CountDownLatch} for processed messages.
   * @see #setProcessedMessageLatch(CountDownLatch) 
   */
  public CountDownLatch getProcessedMessageLatch() {
    return this.latch;
  }
}