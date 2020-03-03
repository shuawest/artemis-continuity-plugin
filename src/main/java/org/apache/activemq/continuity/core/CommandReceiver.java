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

import org.apache.activemq.artemis.api.core.ActiveMQException;
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

public class CommandReceiver implements MessageHandler {

  private static final Logger log = LoggerFactory.getLogger(CommandReceiver.class);

  private final ContinuityService service;
  private final CommandManager commandManager;

  private boolean isStarted = false;

  private ClientSession session = null;
  private ServerLocator locator = null;
  private ClientSessionFactory factory = null;
  private ClientConsumer consumer = null;

  public CommandReceiver(final ContinuityService service, final CommandManager commandManager) {
    this.service = service;
    this.commandManager = commandManager;
  }

  public void start() throws ContinuityException {
    if(isStarted)
      return; 

    prepareSession();
 
    isStarted = true;

    if (log.isDebugEnabled()) {
      log.debug("Finished starting continuity command receiver");
    }
  }

  private void prepareSession() throws ContinuityException {
    try {
      if (this.session == null || session.isClosed()) {

        if(log.isDebugEnabled()) {
          log.debug("Creating local session for commands on '{}' with user '{}'", service.getLocalConnector(), getConfig().getLocalUsername());
        }
        
        this.locator = ActiveMQClient.createServerLocator(false, service.getLocalConnector());
        this.factory = locator.createSessionFactory();
        this.session = factory.createSession(getConfig().getLocalUsername(),
                                             getConfig().getLocalPassword(), 
                                             false, true, true, true, locator.getAckBatchSize());
        session.start();
      }

      if(consumer == null || consumer.isClosed()) {
        log.debug("Creating consumer for commands {}", commandManager.getCommandInQueueName());
        this.consumer = session.createConsumer(commandManager.getCommandInQueueName());
        consumer.setMessageHandler(this);
      }

    } catch (Exception e) {
      String eMessage = "Failed to create session for continuity reciever";
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  public void stop() throws ContinuityException {
    if(!isStarted) 
      return;

    try {
      if(getServer().isStarted()) {
        getServer().getActiveMQServerControl().destroyBridge(commandManager.getCommandOutBridgeName());
      }
      consumer.close();
      session.close();
      factory.close();
      locator.close();
    } catch (final Exception e) {
      String eMessage = "Failed to stop command receiver";
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  public void onMessage(ClientMessage message) {
    String body = message.getBodyBuffer().readString();

    if(log.isDebugEnabled()) {
      log.debug("Received command: {}", body);
    }

    try {
      ContinuityCommand command = ContinuityCommand.fromJSON(body);
      service.handleIncomingCommand(command);
      message.acknowledge(); 

    } catch(ContinuityException e) {
      String msg = String.format("Unable to handle ContinuityCommand: %s", body);
      log.error(msg, e);
    } catch (ParseException e) {
      String msg = String.format("Unable to parse ContinuityCommand: %s", body);
      log.error(msg, e);
    } catch (ActiveMQException e) {
      String msg = String.format("Unable to ackknowledge ContinuityCommand message: %s", body);
      log.error(msg, e);
    }
  }

  private ContinuityConfig getConfig() {
    return service.getConfig();
  }

  private ActiveMQServer getServer() {
    return service.getServer();
  }

  public boolean isStarted() {
    return isStarted;
  }

}
