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
import org.apache.activemq.continuity.ContinuityTestBase;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandReceiverTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(CommandReceiverTest.class);

  @Test
  public void sendCommandTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "CommandReceiverTest.sendCommandTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    ContinuityCommand cmd = new ContinuityCommand();
    cmd.setAction("didsomething");
    cmd.setAddress("myaddress");
    cmd.setQueue("myqueue");
    String cmdJson = ContinuityCommand.toJSON(cmd);

    CommandReceiver receiver = new CommandReceiver(continuityCtx.getService(), continuityCtx.getCommandManager());

    produceAndConsumeMessage(continuityCtx.getConfig(), serverCtx, "cmd-mock", "cmd-mock", receiver, cmdJson, null);

    ArgumentCaptor<ContinuityCommand> cmdCaptor = ArgumentCaptor.forClass(ContinuityCommand.class);
    verify(continuityCtx.getService()).handleIncomingCommand(cmdCaptor.capture());
    ContinuityCommand actualCmd = cmdCaptor.getValue();

    assertThat(actualCmd, notNullValue());
    assertThat(actualCmd.getAction(), equalTo(cmd.getAction()));
    assertThat(actualCmd.getAddress(), equalTo(cmd.getAddress()));
    assertThat(actualCmd.getQueue(), equalTo(cmd.getQueue()));

    serverCtx.getServer().asyncStop(()->{});
  }

  @Test
  public void sendInvalidCommandTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "CommandReceiverTest.sendInvalidCommandTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();
    
    String cmdJson = "{asdfasfs}";

    CommandReceiver receiver = new CommandReceiver(continuityCtx.getService(), continuityCtx.getCommandManager());

    produceAndConsumeMessage(continuityCtx.getConfig(), serverCtx, "cmd-mock", "cmd-mock", receiver, cmdJson, null);

    verify(continuityCtx.getService(), times(0)).handleIncomingCommand(any());

    serverCtx.getServer().asyncStop(()->{});
  }

}
