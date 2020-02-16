/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.continuity.core;

import java.util.Date;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.impl.AckReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckInterceptor {

  private static final Logger log = LoggerFactory.getLogger(AckInterceptor.class);

  public static final String ORIGIN_HEADER = "CONTINUITY_ORIGIN";

  private final ContinuityService service;
  private final ContinuityFlow flow;

  private Boolean isStarted = false;
  private ClientSession session = null;
  private ServerLocator locator = null;
  private ClientSessionFactory factory = null; 
  private ClientProducer producer = null;

  public AckInterceptor(final ContinuityService service, final ContinuityFlow flow) {
    this.service = service;
    this.flow = flow;
  }

  public void start() throws ContinuityException {
    prepareSession();
    isStarted = true;
    log.debug("Finished initializing ack interceptor for {}", flow.getSubjectQueueName());
  }

  public void stop() throws ContinuityException {
    try {
      if(isStarted) {
        producer.close();
        session.close();
        factory.close();
        locator.close();
        isStarted = false;
      }
    } catch (final Exception e) {
      String eMessage = "Failed to stop ack interceptor";
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  private void prepareSession() throws ContinuityException {
    try {
      if (session == null || session.isClosed()) {
        this.locator = ActiveMQClient.createServerLocator(getConfig().getLocalInVmUri());
        this.factory = locator.createSessionFactory();
        this.session = factory.createSession(getConfig().getLocalUsername(),
            getConfig().getLocalPassword(), false, true, true, false, locator.getAckBatchSize());
        
        session.start();

        log.debug("Created session for ack interceptor {}", flow.getSubjectQueueName());
      }

      if(producer == null || producer.isClosed()) {
        this.producer = session.createProducer(flow.getOutflowAcksName());
        log.debug("Created producer for ack interceptor '{}' to '{}'", flow.getSubjectQueueName(), flow.getOutflowAcksName());
      }

    } catch (Exception e) {
      String eMessage = "Failed to create session for ack interceptor to " + flow.getOutflowAcksName();
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  public void handleMessageAcknowledgement(MessageReference ref, AckReason reason) throws ContinuityException {
    Queue sourceQueue = ref.getQueue();
    String queueName = sourceQueue.getName().toString();

    try {
      Date msgTimestamp = new Date(ref.getMessage().getTimestamp());
      Date ackTime = new Date(System.currentTimeMillis());

      String dupId = ref.getMessage().getStringProperty(Message.HDR_DUPLICATE_DETECTION_ID);
      
      if(log.isTraceEnabled()) {
        log.trace("Capturing ack - dupId '{}', msgSent '{}', msgAcked '{}'", dupId, msgTimestamp, ackTime);
      }

      AckInfo ack = new AckInfo();
      ack.setMessageSendTime(msgTimestamp);
      ack.setAckTime(ackTime);
      ack.setMessageUuid(dupId);
      ack.setSourceQueueName(queueName);

      sendAck(ack);
    } catch(Exception e) {
      String msg = String.format("Unable to handle ack for message on queue '%s'", queueName);
      log.error(msg, e);
      throw new ContinuityException(msg, e); 
    }
  }

  public void sendAck(AckInfo ackInfo) throws ContinuityException {
    String body = AckInfo.toJSON(ackInfo); 
    sendAck(body);
  }

  public void sendAck(String body) throws ContinuityException {
    try {
      prepareSession();
      
      if(log.isTraceEnabled()) {
        log.trace("Sending ack info body '{}', origin '{}'", body, getConfig().getSiteId());
      }

      ClientMessage message = session.createMessage(true);
      message.putStringProperty(ORIGIN_HEADER, getConfig().getSiteId());
      message.getBodyBuffer().writeString(body);

      producer.send(message);

    } catch (Exception e) {
      String eMessage = String.format("Failed to send ack info over '%s': %s", flow.getOutflowAcksName(), body);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }    
  }

  private ContinuityConfig getConfig() {
    return service.getConfig();
  }

  public Boolean isStarted() {
    return isStarted;
  }

}
