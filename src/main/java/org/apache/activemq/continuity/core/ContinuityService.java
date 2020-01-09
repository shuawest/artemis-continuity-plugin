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

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityService {

  private static final Logger log = LoggerFactory.getLogger(ContinuityService.class);

  private final ActiveMQServer server;
  private final ContinuityConfig config;

  private CommandManager commandManager;
  private Map<String,ContinuityFlow> flows = new HashMap<String,ContinuityFlow>();

  public ContinuityService(final ActiveMQServer server, final ContinuityConfig config) {
    this.server = server;
    this.config = config;
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


  public void initialize() throws ContinuityException {
    createCommandManager();
  }

  public void registerCommandManager(CommandManager mgr) {
    this.commandManager = mgr;
  }
  
  public CommandManager getCommandManager() {
    return this.commandManager;
  }
  
  public void handleIncomingCommand(ContinuityCommand command) throws ContinuityException {
    if(log.isDebugEnabled()) {
      log.debug("Continuity service received command: {}", command);
    }

    switch(command.getAction()) {
      case ContinuityCommand.ACTION_ADD_QUEUE:
        QueueInfo queueInfo = new QueueInfo();
        queueInfo.setAddressName(command.getAddress());
        queueInfo.setQueueName(command.getQueue());
        if(locateFlow(queueInfo.getQueueName()) == null) {
          createFlow(queueInfo);
        }
        break;

      case ContinuityCommand.ACTION_REMOVE_QUEUE:
        // TODO
        break;

    }
  }

  private void createCommandManager() throws ContinuityException {
    CommandHandler cmdHandler = new CommandHandler(this);
    CommandManager cmdMgr = new CommandManager(this, cmdHandler);
    cmdMgr.initialize();
  }

  
  private void createFlow(QueueInfo queueInfo) throws ContinuityException {
    ContinuityFlow flow = new ContinuityFlow(this, queueInfo);
    flow.initialize();
  }

  public void registerContinuityFlow(String queueName, ContinuityFlow flow) throws ContinuityException {
    flows.put(queueName, flow);
  }
  public ContinuityFlow locateFlow(String queueName) {
    return flows.get(queueName);
  }

  public void handleAddQueue(Queue queue) throws ContinuityException {
    if(isSubjectQueue(queue)) {
      QueueInfo queueInfo = createQueueInfo(queue);

      if(locateFlow(queueInfo.getQueueName()) == null) {
        createFlow(queueInfo);
        
        ContinuityCommand cmd = new ContinuityCommand();
        cmd.setAction(ContinuityCommand.ACTION_ADD_QUEUE);
        cmd.setAddress(queueInfo.getAddressName());
        cmd.setQueue(queueInfo.getQueueName());
        cmd.setUuid(UUID.randomUUID().toString());
        commandManager.sendCommand(cmd);
      }
    }
  }

  public void handleRemoveQueue(Queue queue) throws ContinuityException {
    QueueInfo queueInfo = createQueueInfo(queue);

    ContinuityCommand cmd = new ContinuityCommand();
    cmd.setAction(ContinuityCommand.ACTION_REMOVE_QUEUE);
    cmd.setAddress(queueInfo.getAddressName());
    cmd.setQueue(queueInfo.getQueueName());
    cmd.setUuid(UUID.randomUUID().toString());
    commandManager.sendCommand(cmd);
  }

  private QueueInfo createQueueInfo(Queue queue) {
    QueueInfo queueInfo = new QueueInfo();
    queueInfo.setAddressName(queue.getAddress().toString());
    queueInfo.setQueueName(queue.getName().toString());
    return queueInfo;
  }

  public void stop() throws ContinuityException {
    // TODO
  }

  public ContinuityConfig getConfig() {
    return config;
  }
  public ActiveMQServer getServer() {
    return server;
  }

}
