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
package org.apache.activemq.continuity.plugins;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.core.AckManager;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InflowMirrorPluginTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(InflowMirrorPluginTest.class);

  @Test
  public void mockedMessageTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
 
    String expectedSubjectQueueName = "async-sample1.mock";
    String inflowMirrorName = "async-sample1.in.mirror.mock";
    
    AckManager ackManager = mock(AckManager.class);
    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    when(flowMock.getAckManager()).thenReturn(ackManager);
    Message msgMock = mock(Message.class);
    when(msgMock.getAddress()).thenReturn(inflowMirrorName);
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getService().isInflowMirrorAddress(inflowMirrorName)).thenReturn(true);
    when(continuityCtx.getService().locateFlow(expectedSubjectQueueName)).thenReturn(flowMock);
    
    InflowMirrorPlugin plugin = new InflowMirrorPlugin(continuityCtx.getService());    
    plugin.beforeSend(null, null, msgMock, true, true);

    verify(continuityCtx.getService(), times(1).description("continuityService.isInMirrorAddress not called with expected value")).isInflowMirrorAddress(inflowMirrorName);
    verify(continuityCtx.getService(), times(1).description("continuityService.locateFlow not called with expected value")).locateFlow(expectedSubjectQueueName);
    
    ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class); 
    verify(ackManager, times(1).description("message was not passed to ackManager")).handleInflowMirrorMessage(msgCaptor.capture());
    Message actualMsg = msgCaptor.getValue();
    assertThat("message was not passed to ackManager with expected value", actualMsg, equalTo(msgMock));
  }

  @Test
  public void mockedMessageNonTargetAddressTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
 
    String expectedSubjectQueueName = "async-sample1.mock";
    String inflowMirrorName = "async-sample1.in.mirror.mock";
    
    AckManager ackManager = mock(AckManager.class);
    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    when(flowMock.getAckManager()).thenReturn(ackManager);
    Message msgMock = mock(Message.class);
    when(msgMock.getAddress()).thenReturn(inflowMirrorName);
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getService().isInflowMirrorAddress(inflowMirrorName)).thenReturn(false);
    when(continuityCtx.getService().locateFlow(expectedSubjectQueueName)).thenReturn(flowMock);
    
    InflowMirrorPlugin plugin = new InflowMirrorPlugin(continuityCtx.getService());    
    plugin.beforeSend(null, null, msgMock, true, true);

    verify(continuityCtx.getService(), times(1)).isInflowMirrorAddress(inflowMirrorName);
    verify(continuityCtx.getService(), times(0)).locateFlow(anyString());
    verify(ackManager, times(0)).handleInflowMirrorMessage(any(Message.class));
  }

  @Test
  public void actualMessageTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    MessageHandler handlerMock = mock(MessageHandler.class);
    
    String expectedSubjectQueueName = "async-sample1.mock";
    String inMirrorName = "async-sample1.in.mirror.mock";
    String expectedMessage = "test message";

    AckManager ackManager = mock(AckManager.class);
    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    when(flowMock.getAckManager()).thenReturn(ackManager);
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getService().isInflowMirrorAddress(inMirrorName)).thenReturn(true);
    when(continuityCtx.getService().locateFlow(expectedSubjectQueueName)).thenReturn(flowMock);
    
    InflowMirrorPlugin plugin = new InflowMirrorPlugin(continuityCtx.getService());
    serverCtx.getServer().getConfiguration().registerBrokerPlugin(plugin);
    
    produceAndConsumeMessage(continuityCtx.getConfig(), serverCtx, inMirrorName, inMirrorName, handlerMock, expectedMessage, null);
    
    ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class); 
    verify(ackManager, times(1).description("message was not passed to ackManager")).handleInflowMirrorMessage(msgCaptor.capture());
    Message actualMsg = msgCaptor.getValue();
    String actualMsgBody = actualMsg.toCore().getBodyBuffer().readString();
    assertThat("message was not passed to ackManager with expected value", actualMsgBody, equalTo(expectedMessage));
  }
  
}
