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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.apache.activemq.artemis.core.server.cluster.Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManager {
 
  private static final Logger log = LoggerFactory.getLogger(CommandManager.class);

  public static final String ORIGIN_HEADER = "CONTINUITY_ORIGIN";

  private final ContinuityService service;
  private final CommandHandler commandHandler;

  private final String commandInQueueName; 
  private final String commandOutQueueName;
  private final String commandOutBridgeName;

  private Queue commandInQueue = null;
  private Queue commandOutQueue = null;
  private Bridge commandOutBridge = null;

  private boolean isInitialized = false;
  private ClientSession session = null;
  private ServerLocator locator = null;
  private ClientSessionFactory factory = null; 
  private ClientProducer producer = null;
  private ClientConsumer consumer = null;

  public CommandManager(final ContinuityService service, final CommandHandler commandHandler) {
    this.service = service;
    this.commandHandler = commandHandler;
    this.commandInQueueName = getConfig().getCommandDestinationPrefix() + ".in";
    this.commandOutQueueName = getConfig().getCommandDestinationPrefix() + ".out";
    this.commandOutBridgeName = getConfig().getCommandDestinationPrefix() + ".out.bridge";
  }

  public void initialize() throws ContinuityException {
    if(!isInitialized) {
      commandInQueue = createCommandQueue(commandInQueueName, commandInQueueName);
      prepareSession();
      
      commandOutQueue = createCommandQueue(commandOutQueueName, commandOutQueueName);
      commandOutBridge = createCommandBridge(commandOutBridgeName, getConfig().getRemoteConnectorRef(), commandOutQueueName, commandInQueueName);

      service.registerCommandManager(this);

      isInitialized = true;

      if(log.isDebugEnabled()) {
        log.debug("Finished initializing continuity command manager");
      }
    }
  }

  public void stop() throws ContinuityException {
    try {
      if(isInitialized) {
        getServer().getActiveMQServerControl().destroyBridge(commandOutBridgeName);
        consumer.close();
        producer.close();
        session.close();
        factory.close();
        locator.close();
      }
    } catch (final Exception e) {
      String eMessage = "Failed to stop command manager";
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  private Queue createCommandQueue(String addressName, String queueName) throws ContinuityException {
    log.debug("Creating continuity command queue: address {}, queue {}", addressName, queueName);
    Queue queue = null;
    try {
      queue = getServer().createQueue(new SimpleString(addressName), // address
                                      RoutingType.MULTICAST, // routing type
                                      new SimpleString(queueName), // queue name
                                      null, // filter
                                      true, // durable
                                      false); // temporary

    } catch (final Exception e) {
      log.error("Failed to create continuity command destination", e);
      throw new ContinuityException("Failed to create continuity command destination", e);
    }
    return queue;
  }

  private Bridge createCommandBridge(String bridgeName, String remoteUri, String fromQueueName, String toAddressName) throws ContinuityException {
    Bridge bridge; 
    try {
      BridgeConfiguration bridgeConfig = new BridgeConfiguration()
        .setName(bridgeName)
        .setQueueName(fromQueueName)
        .setForwardingAddress(toAddressName)
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

      bridge = getServer().getClusterManager().getBridges().get(bridgeName);

    } catch (Exception e) {
      String eMessage = String.format("Failed to create command bridge, from '%s' to '%s.%s'", fromQueueName, remoteUri, toAddressName);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
    return bridge;
  }

  private void prepareSession() throws ContinuityException {
    try {
      if (this.session == null || session.isClosed()) {

        if(log.isDebugEnabled()) {
          log.debug("Creating local session for commands on '{}' with user '{}'", getConfig().getLocalInVmUri(), getConfig().getLocalUsername());
        }
        
        this.locator = ActiveMQClient.createServerLocator(getConfig().getLocalInVmUri());
        this.factory = locator.createSessionFactory();
        this.session = factory.createSession(getConfig().getLocalUsername(),
            getConfig().getLocalPassword(), false, true, true, false, locator.getAckBatchSize());
        session.start();
      }

      if(producer == null || producer.isClosed()) {
        log.debug("Creating producer for commands {}", commandOutQueueName);
        this.producer = session.createProducer(commandOutQueueName);
      }

      if(consumer == null || consumer.isClosed()) {
        log.debug("Creating consumer for commands {}", commandInQueueName);
        this.consumer = session.createConsumer(commandInQueueName);
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

  public String getCommandInQueueName() {
    return commandInQueueName;
  }

  public String getCommandOutQueueName() {
    return commandOutQueueName;
  }

  public String getCommandOutBridgeName() {
    return commandOutBridgeName;
  }

  public Queue getCommandInQueue() {
    return commandInQueue;
  }

  public Queue getCommandOutQueue() {
    return commandOutQueue;
  }

  public Bridge getCommandOutBridge() {
    return commandOutBridge;
  }

  public CommandHandler getCommandHandler() {
    return commandHandler;
  }

}
