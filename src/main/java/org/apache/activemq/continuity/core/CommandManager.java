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
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ComponentConfigurationRoutingType;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.QueueQueryResult;
import org.apache.activemq.artemis.core.server.cluster.Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: split out command producer so session is created on demand to send sets of commands 
//       to avoid "AMQ212051: Invalid concurrent session usage. Sessions are not supposed to be used by more than one thread concurrently."
public class CommandManager {

  private static final Logger log = LoggerFactory.getLogger(CommandManager.class);

  private final ContinuityService service;

  private final String commandInQueueName;
  private final String commandOutQueueName;
  private final String commandOutBridgeName;

  private CommandReceiver commandReceiver;

  private Queue commandInQueue = null;
  private Queue commandOutQueue = null;
  private Bridge commandOutBridge = null;

  private boolean isInitialized = false;
  private boolean isStarted = false;


  // TODO: Fix receive ack issue
  public CommandManager(final ContinuityService service) {
    this.service = service;
    this.commandInQueueName = getConfig().getCommandDestinationPrefix() + ".in";
    this.commandOutQueueName = getConfig().getCommandDestinationPrefix() + ".out";
    this.commandOutBridgeName = getConfig().getCommandDestinationPrefix() + ".out.bridge";
  }

  public void initialize() throws ContinuityException {
    if (isInitialized) 
      return;

    service.registerCommandManager(this);

    commandInQueue = createCommandQueue(commandInQueueName, commandInQueueName, RoutingType.ANYCAST);
    commandOutQueue = createCommandQueue(commandOutQueueName, commandOutQueueName, RoutingType.MULTICAST);

    this.commandReceiver = new CommandReceiver(service, this);

    isInitialized = true;

    if (log.isDebugEnabled()) {
      log.debug("Finished initializing continuity command manager");
    }
  }

  public void start() throws ContinuityException {
    if(isStarted)
      return; 

    commandOutBridge = createCommandBridge(commandOutBridgeName, 
                                           getConfig().getRemoteConnectorRef(), 
                                           commandOutQueueName, 
                                           commandInQueueName, 
                                           true);

    commandReceiver.start();

    isStarted = true;

    if (log.isDebugEnabled()) {
      log.debug("Finished starting continuity command manager");
    }
  }

  public void stop() throws ContinuityException {
    if(!isStarted) 
      return;

    try {
      if(getServer().isStarted()) {
        getServer().getActiveMQServerControl().destroyBridge(commandOutBridgeName);
      }
      commandReceiver.stop();

    } catch (final Exception e) {
      String eMessage = "Failed to stop command manager";
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  private Queue createCommandQueue(String addressName, String queueName, RoutingType routingType) throws ContinuityException {
    if(log.isDebugEnabled()) {
      log.debug("Creating continuity command queue: address {}, queue {}", addressName, queueName);
    }
  
    Queue queue = null;
    try {
      if(!queueExists(queueName)) {
        queue = getServer().createQueue(
          new SimpleString(addressName), // address
          routingType, // routing type
          new SimpleString(queueName), // queue name
          null, // filter
          true, // durable
          false); // temporary
      }
    } catch (final Exception e) {
      log.error("Failed to create continuity command destination", e);
      throw new ContinuityException("Failed to create continuity command destination", e);
    }
    return queue;
  }

  private boolean queueExists(final String queueName) throws ContinuityException {
    try {
      final QueueQueryResult queueSearch = getServer().queueQuery(SimpleString.toSimpleString(queueName));
      
      if(log.isTraceEnabled()) {
        log.trace("Checking if queue {} exists: {}", queueName, queueSearch.isExists());
      }
      
      return (queueSearch.isExists());

    } catch (final Exception e) {
      final String eMessage = String.format("Failed check if queue exists: %s", queueName);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  private Bridge createCommandBridge(String bridgeName, String remoteUri, String fromQueueName, String toAddressName, final boolean start) throws ContinuityException {
    Bridge bridge; 
    try {
      BridgeConfiguration bridgeConfig = new BridgeConfiguration()
        .setName(bridgeName)
        .setQueueName(fromQueueName)
        .setForwardingAddress(toAddressName)
        .setHA(true)
        .setUser(getConfig().getRemoteUsername())
        .setPassword(getConfig().getRemotePassword())
        .setRetryInterval(getConfig().getBridgeInterval())
        .setRetryIntervalMultiplier(getConfig().getBridgeIntervalMultiplier())
        .setInitialConnectAttempts(-1)
        .setReconnectAttempts(-1)
        .setRoutingType(ComponentConfigurationRoutingType.ANYCAST)
        .setUseDuplicateDetection(true)
        .setConfirmationWindowSize(10000000)
        .setStaticConnectors(Arrays.asList(getConfig().getRemoteConnectorRef()));

      getServer().deployBridge(bridgeConfig);

      bridge = getServer().getClusterManager().getBridges().get(bridgeName);

      if(!start) {
        bridge.stop();
      }
    } catch (Exception e) {
      String eMessage = String.format("Failed to create command bridge, from '%s' to '%s.%s'", fromQueueName, remoteUri, toAddressName);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
    return bridge;
  }

  // TODO: consider adding batch send command method to reuse a single session
  public void sendCommand(ContinuityCommand command) throws ContinuityException {
    command.setOrigin(getConfig().getSiteId());

    String body = ContinuityCommand.toJSON(command);
    
    sendCommand(body);
  }

  public void sendCommand(String body) throws ContinuityException {
    try {
      if(log.isDebugEnabled()) {
        log.debug("Sending command: {} (Thread: {})", body, Thread.currentThread().getName());
      }

      ServerLocator locator = ActiveMQClient.createServerLocator(false, service.getLocalConnector());
      ClientSessionFactory factory = locator.createSessionFactory();
      ClientSession session = factory.createSession(getConfig().getLocalUsername(),
                                            getConfig().getLocalPassword(), 
                                            false, true, true, false, 
                                            locator.getAckBatchSize());

      ClientProducer producer = session.createProducer(commandOutQueueName);

      session.start();
      
      ClientMessage message = session.createMessage(true);
      message.getBodyBuffer().writeString(body);

      producer.send(message);

      producer.close();
      session.close();
      factory.close();
      locator.close();
      
    } catch (Exception e) {
      String eMessage = "Failed to send command: " + body;
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

  public boolean isStarted() {
    return isStarted;
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

  public CommandReceiver getCommandReceiver() {
    return commandReceiver;
  }

}
