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

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckDivert {

  private static final Logger log = LoggerFactory.getLogger(AckDivert.class);

  public static final String ORIGIN_HEADER = "CONTINUITY_ORIGIN";

  private final ContinuityService service;
  private final ContinuityFlow flow;

  private Boolean isStarted = false;
  private ClientSession session = null;
  private ServerLocator locator = null;
  private ClientSessionFactory factory = null; 
  private ClientProducer producer = null;

  public AckDivert(final ContinuityService service, final ContinuityFlow flow) {
    this.service = service;
    this.flow = flow;
  }

  public void start() throws ContinuityException {
    prepareSession();
    isStarted = true;
    log.debug("Finished initializing ack divert for {}", flow.getSubjectQueueName());
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
      String eMessage = "Failed to stop ack divert";
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

        log.debug("Created session for ack divert {}", flow.getSubjectQueueName());
      }

      if(producer == null || producer.isClosed()) {
        this.producer = session.createProducer(flow.getOutflowAcksName());
        log.debug("Created producer for ack divert '{}' to '{}'", flow.getSubjectQueueName(), flow.getOutflowAcksName());
      }

    } catch (Exception e) {
      String eMessage = "Failed to create session for ack divert to " + flow.getOutflowAcksName();
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  public void sendAck(AckInfo ackInfo) throws ContinuityException {
    String body = AckInfo.toJSON(ackInfo); 
    sendAck(body);
  }

  public void sendAck(String body) throws ContinuityException {
    try {
      prepareSession();
      
      log.debug("Sending ack info body '{}', origin '{}'", body, getServer().getIdentity());

      ClientMessage message = session.createMessage(true);
      message.putStringProperty(ORIGIN_HEADER, getServer().getIdentity());
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

  private ActiveMQServer getServer() {
    return service.getServer();
  }

  public Boolean isStarted() {
    return isStarted;
  }

}
