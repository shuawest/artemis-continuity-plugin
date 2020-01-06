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

import java.util.Arrays;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ComponentConfigurationRoutingType;
import org.apache.activemq.artemis.core.server.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManager {
 
  private static final Logger log = LoggerFactory.getLogger(CommandManager.class);

  public static final String ORIGIN_HEADER = "CONTINUITY_ORIGIN";

  private final ContinuityService service;
  private final CommandHandler commandHandler;

  private Queue commandQueue = null;
  private Queue commandBridgeQueue = null;
  private String bridgeName = null;

  private boolean isInitialized = false;
  private ClientSession session = null;
  private ServerLocator locator = null;
  private ClientSessionFactory factory = null; 
  private ClientProducer producer = null;
  private ClientConsumer consumer = null;

  public CommandManager(final ContinuityService service, final CommandHandler commandHandler) {
    this.service = service;
    this.commandHandler = commandHandler;
  }

  public void initialize() throws ContinuityException {
    if(!isInitialized) {
      String commandQueueName = getConfig().getCommandDestination();
      String commandBridgeQueueName = getConfig().getCommandDestination() + ".bridge";
      createCommandQueue(commandQueueName, commandQueueName);
      createCommandQueue(commandQueueName, commandBridgeQueueName);
      prepareSession();
      createCommandBridge();
      service.registerCommandManager(this);
      isInitialized = true;
      log.debug("Finished initializing continuity manager");
    }
  }

  
  public void stop() throws ContinuityException {
    try {
      if(isInitialized) {
        consumer.close();
        producer.close();
        session.close();
        factory.close();
        locator.close();
        getServer().getActiveMQServerControl().destroyBridge(bridgeName);
      }
    } catch (final Exception e) {
      String eMessage = "Failed to stop command manager";
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  private void createCommandQueue(String addressName, String queueName) throws ContinuityException {
    log.debug("Creating continuity command queue: address {}, queue {}", addressName, queueName);
    
    try {
      this.commandQueue = getServer()
        .createQueue(new SimpleString(addressName), // address
          RoutingType.MULTICAST, // routing type
          new SimpleString(queueName), // queue name
          null, // filter
          true, // durable
          false); // temporary

    } catch (final Exception e) {
      log.error("Failed to create continuity command destination", e);
      throw new ContinuityException("Failed to create continuity command destination", e);
    }
  }

  private void createCommandBridge() throws ContinuityException {
    String fromQueue = getConfig().getCommandDestination() + ".bridge";
    String toAddress = getConfig().getCommandDestination();
    this.bridgeName = getServer().getIdentity() + "-command-Bridge";
    
    try {
      BridgeConfiguration bridgeConfig = new BridgeConfiguration()
        .setName(bridgeName)
        .setQueueName(fromQueue)
        .setForwardingAddress(toAddress)
        .setHA(true)
        .setRetryIntervalMultiplier(1.0)
        .setInitialConnectAttempts(-1)
        .setReconnectAttempts(-1)
        .setRoutingType(ComponentConfigurationRoutingType.MULTICAST)
        .setUseDuplicateDetection(true)
        .setConfirmationWindowSize(10000000)
        .setFilterString(String.format("%s = '%s'", ORIGIN_HEADER, getServer().getIdentity()))
        .setStaticConnectors(Arrays.asList(getConfig().getRemoteConnectorRef()));

      getServer().deployBridge(bridgeConfig);

    } catch (Exception e) {
      String eMessage = String.format("Failed to create divert, from '%s' to '%s.%s'", fromQueue, getConfig().getRemoteConnectorRef(), toAddress);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  private void prepareSession() throws ContinuityException {
    try {
      if (this.session == null || session.isClosed()) {
        log.debug("Creating local session for commands on '{}' with user '{}'", getConfig().getLocalInVmUri(), getConfig().getLocalUsername());
        this.locator = ActiveMQClient.createServerLocator(getConfig().getLocalInVmUri());
        this.factory = locator.createSessionFactory();
        this.session = factory.createSession(getConfig().getLocalUsername(),
            getConfig().getLocalPassword(), false, true, true, false, locator.getAckBatchSize());
        session.start();
      }

      if(producer == null || producer.isClosed()) {
        log.debug("Creating producer for commands {}", getConfig().getCommandDestination());
        this.producer = session.createProducer(getConfig().getCommandDestination());
      }

      if(consumer == null || consumer.isClosed()) {
        log.debug("Creating consumer for commands {}", getConfig().getCommandDestination());
        this.consumer = session.createConsumer(getConfig().getCommandDestination());
        consumer.setMessageHandler(commandHandler);
      }

    } catch (Exception e) {
      String eMessage = "Failed to create session for continuity command queue";
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  public void sendCommand(ContinuityCommand command) throws ContinuityException {
    String body = ContinuityCommand.toJSON(command);
    sendCommand(body);
  }

  public void sendCommand(String body) throws ContinuityException {
    try {
      prepareSession();

      ClientMessage message = session.createMessage(true);
      message.putStringProperty(ORIGIN_HEADER, getServer().getIdentity());
      message.getBodyBuffer().writeString(body);

      producer.send(message);
      
    } catch (Exception e) {
      String eMessage = "Failed send command: " + body;
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

  public boolean isInitialized() {
    return isInitialized;
  }

}
