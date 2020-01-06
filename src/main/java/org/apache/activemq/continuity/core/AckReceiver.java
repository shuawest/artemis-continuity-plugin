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

import java.text.ParseException;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AckReceiver implements MessageHandler {

  private static final Logger log = LoggerFactory.getLogger(AckReceiver.class);

  private final ContinuityService service;
  private final ContinuityFlow flow;

  private Boolean isInitialized = false;
  private ServerLocator locator = null;
  private ClientSessionFactory factory = null;
  private ClientSession session = null;
  private ClientConsumer consumer = null;

  public AckReceiver(final ContinuityService service, final ContinuityFlow flow) {
    this.service = service;
    this.flow = flow;
  }

  public void initialize() throws ContinuityException {
    prepareSession();
    log.debug("Finished initializing ack receiver for {}", flow.getInflowAcksName());
  }

  public void stop() throws ContinuityException {
    try {
      if(isInitialized) {
        consumer.close();
        session.close();
        factory.close();
        locator.close();
      }
    } catch (final Exception e) {
      String eMessage = String.format("Failed to stop ack receiver for %s", flow.getInflowAcksName());
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  private void prepareSession() throws ContinuityException {
    try {
      if (session == null || session.isClosed()) {
        this.locator = ActiveMQClient.createServerLocator(getConfig().getLocalInVmUri());
        this.factory = locator.createSessionFactory();
        this.session = factory.createSession(getConfig().getLocalUsername(), getConfig().getLocalPassword(), false, true, true, false, locator.getAckBatchSize());
        session.start();
        
        if(log.isDebugEnabled())
          log.debug("Created session for ack receiver {}", flow.getInflowAcksName());
      }

      if(consumer == null || consumer.isClosed()) {
        this.consumer = session.createConsumer(flow.getInflowAcksName());
        consumer.setMessageHandler(this);
        
        if(log.isDebugEnabled())
          log.debug("Created consumer for ack receiver {}", flow.getInflowAcksName());
      }

    } catch (Exception e) {
      String eMessage = String.format("Failed to create session for ack receiver from %s", flow.getInflowAcksName());
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  public void onMessage(ClientMessage message) {
    if(log.isDebugEnabled())
      log.debug("Received ack on '{}': {}", flow.getInflowAcksName(), message);
    
    String ackBody = message.getBodyBuffer().readString(); 

    try { 
      AckInfo ack = AckInfo.fromJSON(ackBody);
      flow.getAckManager().handleAck(ack);

    } catch(ParseException parseException) {
      String eMessage = String.format("Unable to parse incoming ack: %s", ackBody);
      log.error(eMessage, parseException);
    } catch(ContinuityException continuityException) {
      String eMessage = String.format("Unable to handle ack: %s", ackBody);
      log.error(eMessage, continuityException);
    }
  }

  private ContinuityConfig getConfig() {
    return service.getConfig();
  }

  private ActiveMQServer getServer() {
    return service.getServer();
  }

}
