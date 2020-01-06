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
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManagerTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(CommandManagerTest.class);

  @Test
  public void initializeTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();
    
    CommandManager manager = new CommandManager(continuityCtx.getService(), continuityCtx.getCommandHandler());
    manager.initialize();

    Queue cmdQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString("artemis.continuity.commands"));
    assertThat("command queue not created", cmdQueue, notNullValue());
    assertThat("command listener not subscribed", cmdQueue.getConsumerCount(), equalTo(1));

    manager.stop();
  }

  @Test
  public void sendCommandTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();
    
    CommandManager manager = new CommandManager(continuityCtx.getService(), continuityCtx.getCommandHandler());
    manager.initialize();
    
    manager.sendCommand("test message");
    Thread.sleep(100);
 
    verifyMessage("test message", serverCtx.getServer().getIdentity(), 
      continuityCtx, times(1).description("Failed to receive command"));

    manager.stop();
  }

  @Test
  public void bridgeCommandTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx1 = createMockContext(serverCtx1, "primary", 1);
    serverCtx1.getServer().start();

    ServerContext serverCtx2 = createServerContext("broker2-noplugin.xml", "backup-server", "myuser", "mypass");
    ContinuityContext continuityCtx2 = createMockContext(serverCtx2, "backup", 2);
    serverCtx2.getServer().start();

    CommandManager manager1 = new CommandManager(continuityCtx1.getService(), continuityCtx1.getCommandHandler());
    manager1.initialize();

    CommandManager manager2 = new CommandManager(continuityCtx2.getService(), continuityCtx2.getCommandHandler());
    manager2.initialize();

    manager1.sendCommand("test message from primary");
    Thread.sleep(200);

    verifyMessage("test message from primary", serverCtx1.getServer().getIdentity(), 
      continuityCtx1, times(1).description("Failed to receive command on primary from primary"));
    verifyMessage("test message from primary", serverCtx1.getServer().getIdentity(), 
      continuityCtx2, times(1).description("Failed to receive command on backup from primary"));
      
    manager1.stop();
    manager2.stop();
  }

  @Test
  public void bridgeCommandBidirectionalTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx1 = createMockContext(serverCtx1, "primary", 1);
    serverCtx1.getServer().start();

    ServerContext serverCtx2 = createServerContext("broker2-noplugin.xml", "backup-server", "myuser", "mypass");
    ContinuityContext continuityCtx2 = createMockContext(serverCtx2, "backup", 2);
    serverCtx2.getServer().start();

    CommandManager manager1 = new CommandManager(continuityCtx1.getService(), continuityCtx1.getCommandHandler());
    manager1.initialize();

    CommandManager manager2 = new CommandManager(continuityCtx2.getService(), continuityCtx2.getCommandHandler());
    manager2.initialize();

    manager1.sendCommand("test message from primary");
    manager2.sendCommand("test message from backup");
    Thread.sleep(200);
 
    verifyMessage("test message from primary", serverCtx1.getServer().getIdentity(), 
       continuityCtx1, times(2).description("Failed to receive command on primary from primary"));
    verifyMessage("test message from primary", serverCtx1.getServer().getIdentity(), 
      continuityCtx2, times(1).description("Failed to receive command on backup from primary"));

    verifyMessage("test message from backup", serverCtx2.getServer().getIdentity(), 
      continuityCtx1, description("Failed to receive command on primary from backup"));
    verifyMessage("test message from backup", serverCtx2.getServer().getIdentity(), 
      continuityCtx2, description("Failed to receive command on backup from backup"));
      
    manager1.stop();
    manager2.stop();
  }

  // TODO
  // sharedJournalNodeFailureTest - primary and backup 2 node shared-journal clusters
  // sharedNothingNodeFailureTest - primary and backup 3 node shared-nothing clusters

  private void verifyMessage(String body, String originHeader, ContinuityContext cctx, VerificationMode mode) {
    ArgumentCaptor<ClientMessage> msgCaptor = ArgumentCaptor.forClass(ClientMessage.class);
    verify(cctx.getCommandHandler(), mode).onMessage(msgCaptor.capture());
    ClientMessage msg = msgCaptor.getValue();
    String actualBody = msg.getReadOnlyBodyBuffer().readString().toString();
    String actualOrigin = msg.getStringProperty(CommandManager.ORIGIN_HEADER); 

    log.debug("Received message - origin '{}', body: {}", actualOrigin, actualBody);

    assertThat(msg.getReadOnlyBodyBuffer().readString().toString(), equalTo(body));
    assertThat(msg.getStringProperty(CommandManager.ORIGIN_HEADER), equalTo(originHeader));
  }

}
