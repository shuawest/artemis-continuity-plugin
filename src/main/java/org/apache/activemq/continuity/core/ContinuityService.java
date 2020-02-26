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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.remoting.Acceptor;
import org.apache.activemq.continuity.management.ContinuityManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityService {

  private static final Logger log = LoggerFactory.getLogger(ContinuityService.class);

  private final ActiveMQServer server;
  private final ContinuityConfig config;
  private final ContinuityManagementService mgmt;

  private boolean isActivated = false;
  private boolean isDelivering = false;
  private boolean isInitializing = false;
  private boolean isInitialized = false;
  private boolean isStarting = false;
  private boolean isStarted = false;

  private CommandManager commandManager;
  private Map<String, ContinuityFlow> flows = new HashMap<String, ContinuityFlow>();

  private TransportConfiguration internalAcceptorConfig;
  private TransportConfiguration externalAcceptorConfig;

  public ContinuityService(final ActiveMQServer server, final ContinuityConfig config) {
    this.server = server;
    this.config = config;
    this.mgmt = new ContinuityManagementService(server.getManagementService());
  }

  public synchronized void initialize() throws ContinuityException {
    if (isInitialized || isInitializing)
      return;

    isInitializing = true;

    if (log.isDebugEnabled()) {
      log.debug("Initializing continuity service");
    }

    if (this.commandManager == null) {
      CommandManager cmdMgr = new CommandManager(this);
      cmdMgr.initialize();
    }

    internalAcceptorConfig = locateAcceptorTransportConfig(getConfig().getInternalAcceptorName());
    externalAcceptorConfig = locateAcceptorTransportConfig(getConfig().getExternalAcceptorName());

    isInitialized = true;
    isInitializing = false;
  }

  public void start() throws ContinuityException {
    if (getConfig().isSiteActiveByDefault()) {
      isActivated = true;
      isDelivering = true;
    } else {
      stopNonContinuityAcceptors();
      isActivated = false;
      isDelivering = false;
    }

    commandManager.start();

    for (ContinuityFlow flow : flows.values()) {
      flow.start();
    }

    getManagement().registerContinuityService(this);

    // Notify peer sites that this broker has started
    ContinuityCommand cmdStarted = new ContinuityCommand();
    cmdStarted.setAction(ContinuityCommand.ACTION_BROKER_CONNECT);
    commandManager.sendCommand(cmdStarted);

    isStarted = true;

    if (log.isInfoEnabled()) {
      log.info("Continuity Plugin Started");
    }
  }

  public void registerCommandManager(CommandManager mgr) {
    this.commandManager = mgr;
  }

  public void stop() throws ContinuityException {
    commandManager.stop();
    for (ContinuityFlow flow : flows.values()) {
      flow.stop();
    }
    getManagement().unregisterContinuityService();
    isStarted = false;
  }

  public void handleAddQueue(Queue queue) throws ContinuityException {
    if (isSubjectQueue(queue)) {
      initialize();

      QueueInfo queueInfo = extractQueueInfo(queue);

      if (locateFlow(queueInfo.getQueueName()) == null) {

        ContinuityFlow flow = createFlow(queueInfo);
        if (isStarted) {
          flow.start();
        }

        if (commandManager != null && commandManager.isStarted()) {
          ContinuityCommand cmd = new ContinuityCommand();
          cmd.setAction(ContinuityCommand.ACTION_ADD_QUEUE);
          cmd.setAddress(queueInfo.getAddressName());
          cmd.setQueue(queueInfo.getQueueName());
          cmd.setRoutingType(queueInfo.getRoutingType());
          commandManager.sendCommand(cmd);
        }
      }
    }
  }

  public void handleRemoveQueue(Queue queue) throws ContinuityException {
    if (commandManager != null && commandManager.isStarted() && isSubjectQueue(queue)) {
      QueueInfo queueInfo = extractQueueInfo(queue);

      String queueName = queueInfo.getQueueName();
      ContinuityFlow flow = locateFlow(queueName);
      if (flow != null) {
        flow.destroyFlow();
        flows.remove(queueName);

        ContinuityCommand cmd = new ContinuityCommand();
        cmd.setAction(ContinuityCommand.ACTION_REMOVE_QUEUE);
        cmd.setAddress(queueInfo.getAddressName());
        cmd.setQueue(queueInfo.getQueueName());
        commandManager.sendCommand(cmd);
      }
    }
  }

  private ContinuityFlow createFlow(QueueInfo queueInfo) throws ContinuityException {
    ContinuityFlow flow = new ContinuityFlow(this, queueInfo);
    flow.initialize();
    return flow;
  }

  public void registerContinuityFlow(String queueName, ContinuityFlow flow) throws ContinuityException {
    flows.put(queueName, flow);
  }

  public ContinuityFlow locateFlow(String queueName) {
    return flows.get(queueName);
  }

  private QueueInfo extractQueueInfo(Queue queue) {
    QueueInfo queueInfo = new QueueInfo();
    queueInfo.setAddressName(queue.getAddress().toString());
    queueInfo.setQueueName(queue.getName().toString());
    queueInfo.setRoutingType(queue.getRoutingType().toString());
    return queueInfo;
  }

  public void handleIncomingCommand(ContinuityCommand command) throws ContinuityException {

    switch (command.getAction()) {
      case ContinuityCommand.ACTION_ACTIVATE_SITE:
        activateSite(getConfig().getActivationTimeout());
        break;

      case ContinuityCommand.NOTIF_SITE_ACTIVATED:
        deactivateSite();
        break;

      case ContinuityCommand.NOTIF_OUTFLOW_EXHAUSTED:
        startInflowAcksConsumedWatcher();
        break;

      case ContinuityCommand.ACTION_BROKER_CONNECT:
        for (ContinuityFlow flow : flows.values()) {
          ContinuityCommand cmd = new ContinuityCommand();
          cmd.setAction(ContinuityCommand.ACTION_ADD_QUEUE);
          cmd.setAddress(flow.getSubjectAddressName());
          cmd.setQueue(flow.getSubjectQueueName());
          cmd.setRoutingType(flow.getSubjectQueueRoutingType());
          commandManager.sendCommand(cmd);
        }
        break;

      case ContinuityCommand.ACTION_ADD_QUEUE:
        QueueInfo queueInfo = new QueueInfo();
        queueInfo.setAddressName(command.getAddress());
        queueInfo.setQueueName(command.getQueue());
        queueInfo.setRoutingType(command.getRoutingType());
        if (locateFlow(queueInfo.getQueueName()) == null) {
          ContinuityFlow flow = createFlow(queueInfo);
          flow.start();
        }
        break;

      case ContinuityCommand.ACTION_REMOVE_QUEUE:
        String queueName = command.getQueue();
        Queue queue = getServer().locateQueue(SimpleString.toSimpleString(queueName));
        handleRemoveQueue(queue);
        break;
    }
  }

  public void activateSite(long timeout) throws ContinuityException {
    isActivated = true;

    startNonContinuityAcceptors();

    ContinuityCommand cmd = new ContinuityCommand();
    cmd.setAction(ContinuityCommand.NOTIF_SITE_ACTIVATED);
    getCommandManager().sendCommand(cmd);

    // start timeout executor for exhausted notification to start delivery
    startActivationTimeoutExecutor(timeout);
  }

  public void stopNonContinuityAcceptors() throws ContinuityException {
    Set<TransportConfiguration> acceptorConfigs = getServer().getConfiguration().getAcceptorConfigurations();
    for (TransportConfiguration acceptorConfig : acceptorConfigs) {
      String acceptorName = acceptorConfig.getName();

      if (!acceptorName.equals(getConfig().getExternalAcceptorName())
          && !acceptorName.equals(getConfig().getInternalAcceptorName())) {
        Acceptor acceptor = getServer().getRemotingService().getAcceptor(acceptorConfig.getName());
        if (acceptor != null) {
          try {
            acceptor.stop();
          } catch (Exception e) {
            if(log.isWarnEnabled()) {
              log.warn("Unable to stop acceptor '{}'", acceptorName);
            }
          }

          if (log.isDebugEnabled()) {
            log.debug("Paused acceptor '{}'", acceptorName);
          }
        }
      }
    }
  }

  public void startNonContinuityAcceptors() throws ContinuityException {
    Set<TransportConfiguration> acceptorConfigs = getServer().getConfiguration().getAcceptorConfigurations();
    for (TransportConfiguration acceptorConfig : acceptorConfigs) {
      String acceptorName = acceptorConfig.getName();

      if (!acceptorName.equals(getConfig().getExternalAcceptorName())
          && !acceptorName.equals(getConfig().getInternalAcceptorName())) {
        Acceptor acceptor = getServer().getRemotingService().getAcceptor(acceptorConfig.getName());
        
        try {
          if(acceptor != null) {
            acceptor.start();

            if(log.isDebugEnabled()) {
              log.debug("Unpaused acceptor '{}'", acceptorName);
            }
          }
        } catch (Exception e) {
          String msg = String.format("Unable to unpause non-continuity acceptor: %s", acceptorName);
          log.error(msg, e); 
          throw new ContinuityException(msg, e);
        }
      }
    }
  }

  public void stopNonContinuityDelivery() throws ContinuityException {
    Set<RemotingConnection> connections = getServer().getRemotingService().getConnections();
    for(RemotingConnection conn : connections) {
      TransportConfiguration connectionTransportConfig = conn.getTransportConnection().getConnectorConfig();
      boolean isInternalAcceptorConn = isTransportEqual(connectionTransportConfig, internalAcceptorConfig);
      boolean isExternalAcceptorConn = isTransportEqual(connectionTransportConfig, externalAcceptorConfig);

      if(!isInternalAcceptorConn && !isExternalAcceptorConn) {
        conn.disconnect(false);
        conn.destroy();
      }
    }
  }
  
  private boolean isTransportEqual(TransportConfiguration t1, TransportConfiguration t2) {
    if(!t1.isEquivalent(t2))
      return false;

    Object t1Port = t1.getParams().get("port");
    Object t2Port = t2.getParams().get("port");
    if(t1Port != null && t2Port != null && t1Port.toString().equals(t2Port.toString())) {
      return true;
    }

    Object t1ServerId = t1.getParams().get("serverId");
    Object t2ServerId = t2.getParams().get("serverId");
    if(t1ServerId != null && t2ServerId != null && t1ServerId.toString().equals(t2ServerId.toString())) {
      return true;
    }

    return false;
  }

  public OutflowExhaustedWatcher startOutflowWatcher() throws ContinuityException {
    // wait until outflow mirror and ack queue exhausted, then notify peer site
    OutflowExhaustedWatcher watcher = 
      new OutflowExhaustedWatcher(
        this, getConfig().getOutflowExhaustedPollDuration(), 
        getServer().getScheduledPool(), 
        getServer().getExecutorFactory().getExecutor());

    watcher.start();
    return watcher;
  }

  public InflowAcksConsumedWatcher startInflowAcksConsumedWatcher() throws ContinuityException {
    // wait until inflow acks consumed, then start subject queue delivery
    InflowAcksConsumedWatcher watcher = 
      new InflowAcksConsumedWatcher(
        this, getConfig().getInflowAcksConsumedPollDuration(),
        getServer().getScheduledPool(), 
        getServer().getExecutorFactory().getExecutor());

    watcher.start();
    return watcher;
  }

  public ActivationTimeoutExecutor startActivationTimeoutExecutor(long timeout) throws ContinuityException {
    ActivationTimeoutExecutor activationExecutor = 
      new ActivationTimeoutExecutor(
        this, timeout,
        getServer().getScheduledPool(), 
        getServer().getExecutorFactory().getExecutor());
    
    activationExecutor.start();
    return activationExecutor;
  }

  public void startSubjectQueueDelivery() throws ContinuityException {
    if(isActivated) {
      for(ContinuityFlow flow : flows.values()) {
        flow.startSubjectQueueDelivery();
      }
      isDelivering = true;
    }
  }

  public void stopSubjectQueueDelivery() throws ContinuityException {
    for(ContinuityFlow flow : flows.values()) {
      flow.stopSubjectQueueDelivery();
    }
    isDelivering = false;
  }

  public void deactivateSite() throws ContinuityException {
    isActivated = false;

    // stop delivering messages to subject queues from staging
    stopSubjectQueueDelivery();
    // stop new connections from being accepted
    stopNonContinuityAcceptors();
    // kill existing connections on non-continuity acceptors
    stopNonContinuityDelivery();
    // wait for mirrors to be emptied, then notify peer site
    startOutflowWatcher();
  }

  public void handleOutflowExhausted() throws ContinuityException {
    ContinuityCommand cmd = new ContinuityCommand();
    cmd.setAction(ContinuityCommand.NOTIF_OUTFLOW_EXHAUSTED);
    getCommandManager().sendCommand(cmd);
  }

  public void handleInflowQueuesConsumed() throws ContinuityException {
    startSubjectQueueDelivery();
  }


  private TransportConfiguration locateAcceptorTransportConfig(String name) throws ContinuityException {
    Set<TransportConfiguration> acceptorConfigs = getServer().getConfiguration().getAcceptorConfigurations();
    for (TransportConfiguration acceptorConfig : acceptorConfigs) {
      String acceptorName = acceptorConfig.getName();
      if(name.equals(acceptorName)) {
        return acceptorConfig;
      }
    }

    // if not found throw exception
    throw new ContinuityException(String.format("Unable to find acceptor configuration '%s'", name));
  }

  public List<Queue> getOutflowQueues() {
    List<Queue> outflowQueues = new ArrayList<Queue>();
    Collection<ContinuityFlow> flows = getFlows();
    for (ContinuityFlow flow : flows) {
      Queue outflowMirrorQueue = getServer().locateQueue(SimpleString.toSimpleString(flow.getOutflowMirrorName()));
      Queue outflowAcksQueue = getServer().locateQueue(SimpleString.toSimpleString(flow.getOutflowAcksName()));
      outflowQueues.add(outflowMirrorQueue);
      outflowQueues.add(outflowAcksQueue);
    }
    return outflowQueues;
  }

  public List<Queue> getInfowAcksQueues() {
    List<Queue> inflowAcksQueues = new ArrayList<Queue>();
    Collection<ContinuityFlow> flows = getFlows();
    for (ContinuityFlow flow : flows) {
      Queue infowAcksQueue = getServer().locateQueue(SimpleString.toSimpleString(flow.getInflowAcksName()));
      inflowAcksQueues.add(infowAcksQueue);
    }
    return inflowAcksQueues;
  }

  public ContinuityConfig getConfig() {
    return config;
  }
  public ActiveMQServer getServer() {
    return server;
  }

  public boolean isInitializing() {
    return isInitializing;
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  public boolean isStarting() {
    return isStarting;
  }

  public boolean isStarted() {
    return isStarted;
  }

  public boolean isActivated() {
    return isActivated;
  }

  public boolean isDelivering() {
    return isDelivering;
  }

  private static Pattern continuityAddressPattern;

  public boolean isSubjectAddress(String addressName) {
    if(continuityAddressPattern == null) {
      continuityAddressPattern = Pattern.compile(".*(" + 
        config.getOutflowMirrorSuffix() + "|" + 
        config.getOutflowAcksSuffix() + "|" + 
        config.getInflowMirrorSuffix() + "|" + 
        config.getInflowAcksSuffix() + "|" + 
        config.getCommandDestinationPrefix() + ").*");
    }

    boolean isContinuityAddress = continuityAddressPattern.matcher(addressName).matches();

    return !isContinuityAddress;
  }

  public boolean isSubjectQueue(String queueName) {
    Queue queue = getServer().locateQueue(SimpleString.toSimpleString(queueName));
    return isSubjectQueue(queue);
  }

  public boolean isSubjectQueue(Queue queue) {
    if(queue == null) // nondurable queues don't exist
      return false;

    String addressName = queue.getAddress().toString();
    return (isSubjectAddress(addressName) && queue.isDurable() && !queue.isTemporary());
  }

  public boolean isOutflowMirrorAddress(String addressName) {
    return addressName.endsWith(config.getOutflowMirrorSuffix());
  }

  public boolean isOutflowAcksAddress(String addressName) {
    return addressName.endsWith(config.getOutflowAcksSuffix());
  }

  public boolean isInflowMirrorAddress(String addressName) {
    return addressName.endsWith(config.getInflowMirrorSuffix());
  }

  public boolean isInflowAcksAddress(String addressName) {
    return addressName.endsWith(config.getInflowAcksSuffix());
  }

  public CommandManager getCommandManager() {
    return this.commandManager;
  }

  public Collection<ContinuityFlow> getFlows() {
    return flows.values();
  }

  public ContinuityManagementService getManagement() {
    return mgmt;
  }

}
