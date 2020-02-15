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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.continuity.management.ContinuityManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityService {

  private static final Logger log = LoggerFactory.getLogger(ContinuityService.class);

  private final ActiveMQServer server;
  private final ContinuityConfig config;
  private final ContinuityManagementService mgmt; 

  private boolean isInitializing = false;
  private boolean isInitialized = false;
  private boolean isStarting = false;
  private boolean isStarted = false;

  private CommandManager commandManager;
  private Map<String,ContinuityFlow> flows = new HashMap<String,ContinuityFlow>();

  public ContinuityService(final ActiveMQServer server, final ContinuityConfig config) {
    this.server = server;
    this.config = config;
    this.mgmt = new ContinuityManagementService(server.getManagementService());
  }

  public synchronized void initialize() throws ContinuityException {
    if(isInitialized || isInitializing)
      return;

    isInitializing = true;

    if(log.isDebugEnabled()) {
      log.debug("Initializing continuity service");
    }

    CommandReceiver cmdReceiver = new CommandReceiver(this);
    CommandManager cmdMgr = new CommandManager(this, cmdReceiver);
    cmdMgr.initialize();

    isInitialized = true;
    isInitializing = false;
  }

  public void start() throws ContinuityException {
    commandManager.start();

    for(ContinuityFlow flow : flows.values()) {
      flow.start();
    }

    getManagement().registerContinuityService(this);

    // Notify peer sites that this broker has started
    ContinuityCommand cmdStarted = new ContinuityCommand();
    cmdStarted.setAction(ContinuityCommand.ACTION_BROKER_CONNECT);
    commandManager.sendCommand(cmdStarted);

    isStarted = true;

    if(log.isInfoEnabled()) {
      log.info("Continuity Plugin Started");
    }
  }

  public void registerCommandManager(CommandManager mgr) {
    this.commandManager = mgr;
  }

  public void stop() throws ContinuityException {
    commandManager.stop();
    for(ContinuityFlow flow : flows.values()) {
      flow.stop();
    }
    isStarted = false;
  }

  public void handleAddQueue(Queue queue) throws ContinuityException {
    if(isSubjectQueue(queue)) {
      initialize();

      QueueInfo queueInfo = extractQueueInfo(queue);

      if(locateFlow(queueInfo.getQueueName()) == null) {
        
        createFlow(queueInfo);
        
        if(commandManager != null && commandManager.isStarted()) {
          ContinuityCommand cmd = new ContinuityCommand();
          cmd.setAction(ContinuityCommand.ACTION_ADD_QUEUE);
          cmd.setAddress(queueInfo.getAddressName());
          cmd.setQueue(queueInfo.getQueueName());
          commandManager.sendCommand(cmd);
        }
      }
    }
  }

  public void handleRemoveQueue(Queue queue) throws ContinuityException {
    if(commandManager != null && commandManager.isStarted() && isSubjectQueue(queue)) {
      QueueInfo queueInfo = extractQueueInfo(queue);

      ContinuityCommand cmd = new ContinuityCommand();
      cmd.setAction(ContinuityCommand.ACTION_REMOVE_QUEUE);
      cmd.setAddress(queueInfo.getAddressName());
      cmd.setQueue(queueInfo.getQueueName());
      commandManager.sendCommand(cmd);
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
    return queueInfo;
  }


  public void handleIncomingCommand(ContinuityCommand command) throws ContinuityException {
    if(log.isDebugEnabled()) {
      log.debug("Received command: {}", command);
    }

    switch(command.getAction()) {
      case ContinuityCommand.ACTION_ACTIVATE_SITE: 
        activateSite();
        break;

      case ContinuityCommand.ACTION_BROKER_CONNECT:
        for(ContinuityFlow flow : flows.values()) {
          ContinuityCommand cmd = new ContinuityCommand();
          cmd.setAction(ContinuityCommand.ACTION_ADD_QUEUE);
          cmd.setAddress(flow.getSubjectAddressName());
          cmd.setQueue(flow.getSubjectQueueName());
          commandManager.sendCommand(cmd);
        }
        break; 

      case ContinuityCommand.ACTION_ADD_QUEUE:
        QueueInfo queueInfo = new QueueInfo();
        queueInfo.setAddressName(command.getAddress());
        queueInfo.setQueueName(command.getQueue());
        if(locateFlow(queueInfo.getQueueName()) == null) {
          ContinuityFlow flow = createFlow(queueInfo);
          flow.start();
        }
        break;

      case ContinuityCommand.ACTION_REMOVE_QUEUE:
        // TODO
        break;
    }
  }

  public void activateSite() throws ContinuityException {
    for(ContinuityFlow flow : flows.values()) {
      flow.startSubjectQueueDelivery();
    }
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

  public boolean isSubjectAddress(String addressName) {
    return config.getAddresses().contains(addressName);
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
