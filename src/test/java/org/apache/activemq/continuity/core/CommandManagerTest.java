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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManagerTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(CommandManagerTest.class);

  @Test
  public void initializeTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "CommandManagerTest.initializeTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();
    
    CommandManager manager = new CommandManager(continuityCtx.getService());
    manager.initialize();
    manager.start();

    assertThat("isinitialized", manager.isInitialized(), equalTo(true));
    assertThat("getCommandInQueueName", manager.getCommandInQueueName(), equalTo("artemis.continuity.commands.in"));
    assertThat("getCommandOutQueueName", manager.getCommandOutQueueName(), equalTo("artemis.continuity.commands.out"));
    assertThat("getCommandOutBridgeName", manager.getCommandOutBridgeName(), equalTo("artemis.continuity.commands.out.bridge"));
    assertThat("getCommandInQueue", manager.getCommandInQueue(), notNullValue());
    assertThat("getCommandOutQueue", manager.getCommandOutQueue(), notNullValue());
    assertThat("getCommandOutBridge", manager.getCommandOutBridge(), notNullValue());

    Queue cmdQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString("artemis.continuity.commands.in"));
    assertThat("command in queue not created", cmdQueue, notNullValue());
    assertThat("command in listener not subscribed", cmdQueue.getConsumerCount(), equalTo(1));

    manager.stop();
    serverCtx.getServer().asyncStop(()->{});
  }

  @Test
  public void bridgeCommandTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-noplugin.xml", "CommandManagerTest.bridgeCommandTest", "myuser", "mypass");
    ContinuityContext continuityCtx1 = createMockContext(serverCtx1, "primary", 1);
    serverCtx1.getServer().start();

    ServerContext serverCtx2 = createServerContext("broker2-noplugin.xml", "CommandManagerTest.bridgeCommandTest", "myuser", "mypass");
    ContinuityContext continuityCtx2 = createMockContext(serverCtx2, "backup", 2);
    serverCtx2.getServer().start();

    CommandManager manager1 = new CommandManager(continuityCtx1.getService());
    CommandManager manager2 = new CommandManager(continuityCtx2.getService());
    manager1.initialize();
    manager2.initialize();
    manager1.start();
    manager2.start();

    ContinuityCommand expectedCmd = new ContinuityCommand();
    expectedCmd.setAction("test from primary");

    manager1.sendCommand(expectedCmd);
    Thread.sleep(200L);

    verifyMessage(continuityCtx2, "test from primary", continuityCtx1.getConfig().getSiteId(), 
        1, "Failed to receive command on backup from primary");
    verifyMessage(continuityCtx1, "test from primary", continuityCtx1.getConfig().getSiteId(), 
        0, "should not received on primary from primary message from primary");
        
    //handleIncomingCommand(ContinuityCommand command)
      
    manager1.stop();
    manager2.stop();
    serverCtx1.getServer().asyncStop(()->{});
    serverCtx2.getServer().asyncStop(()->{});
  }

  @Test
  public void bridgeCommandFromBackupTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-noplugin.xml", "CommandManagerTest.bridgeCommandFromBackupTest", "myuser", "mypass");
    ContinuityContext continuityCtx1 = createMockContext(serverCtx1, "primary", 1);
    serverCtx1.getServer().start();

    ServerContext serverCtx2 = createServerContext("broker2-noplugin.xml", "CommandManagerTest.bridgeCommandFromBackupTest", "myuser", "mypass");
    ContinuityContext continuityCtx2 = createMockContext(serverCtx2, "backup", 2);
    serverCtx2.getServer().start();

    CommandManager manager1 = new CommandManager(continuityCtx1.getService());
    CommandManager manager2 = new CommandManager(continuityCtx2.getService());
    manager1.initialize();
    manager2.initialize();
    manager1.start();
    manager2.start();

    ContinuityCommand expectedCmd = new ContinuityCommand();
    expectedCmd.setAction("test from backup");

    manager2.sendCommand(expectedCmd);
    Thread.sleep(200L);

    verifyMessage(continuityCtx1, "test from backup", continuityCtx2.getConfig().getSiteId(), 
        1, "Failed to receive command on primary from backup");
    verifyMessage(continuityCtx2, "test from backup", continuityCtx1.getConfig().getSiteId(), 
        0, "should not receive on backup from backup");
      
    manager1.stop();
    manager2.stop();
    serverCtx1.getServer().asyncStop(()->{});
    serverCtx2.getServer().asyncStop(()->{});
  }

  // TODO
  // sharedJournalNodeFailureTest - primary and backup 2 node shared-journal clusters
  // sharedNothingNodeFailureTest - primary and backup 3 node shared-nothing clusters

  private void verifyMessage(ContinuityContext cctx, String expectedAction, String expectedOrigin, int count, String description) throws ContinuityException {
    ArgumentCaptor<ContinuityCommand> cmdCaptor = ArgumentCaptor.forClass(ContinuityCommand.class);
    if(count == 0) { 
      verify(cctx.getService(), times(count).description(description)).handleIncomingCommand(any());
    } else {
      verify(cctx.getService(), times(count).description(description)).handleIncomingCommand(cmdCaptor.capture());
      ContinuityCommand cmd = cmdCaptor.getValue();
      String actualBody = cmd.getAction();
      String actualOrigin = cmd.getOrigin();

      log.debug("Received message - origin '{}', body: {}\n\n", expectedOrigin, actualBody);

      assertThat(actualBody, equalTo(expectedAction));
      assertThat(actualOrigin, equalTo(expectedOrigin));
    }
  }

}
