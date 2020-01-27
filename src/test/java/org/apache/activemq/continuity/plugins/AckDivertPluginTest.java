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
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.UUID;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.core.AckDivert;
import org.apache.activemq.continuity.core.AckInfo;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckDivertPluginTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(AckDivertPluginTest.class);

  @Test
  public void mockedMessageTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
 
    AckDivert divertMock = mock(AckDivert.class);
    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    Queue queueMock = mock(Queue.class);
    MessageReference refMock = mock(MessageReference.class);
    Message msgMock = mock(Message.class);

    String expectedQueueName = "async-sample1";
    String expectedUuid = UUID.randomUUID().toString();
    Date preAckTime = new Date(System.currentTimeMillis());

    when(flowMock.getAckDivert()).thenReturn(divertMock);
    when(continuityCtx.getService().locateFlow(expectedQueueName)).thenReturn(flowMock);
    when(continuityCtx.getService().isSubjectQueue(any(Queue.class))).thenReturn(true);
    when(queueMock.isDurable()).thenReturn(true);
    when(queueMock.isTemporary()).thenReturn(false);
    when(queueMock.getName()).thenReturn(SimpleString.toSimpleString(expectedQueueName));
    when(refMock.getQueue()).thenReturn(queueMock);
    when(refMock.getMessage()).thenReturn(msgMock);
    when(msgMock.getStringProperty(Message.HDR_DUPLICATE_DETECTION_ID)).thenReturn(expectedUuid);

    AckDivertPlugin plugin = new AckDivertPlugin(continuityCtx.getService());
    plugin.messageAcknowledged(refMock, null, null);

    ArgumentCaptor<AckInfo> ackCaptor = ArgumentCaptor.forClass(AckInfo.class);
    verify(divertMock, times(1)).sendAck(ackCaptor.capture());
    AckInfo actualAck = ackCaptor.getValue();
    
    assertThat("ack was null", actualAck, notNullValue());
    assertThat("msg send time was null", actualAck.getMessageSendTime(), notNullValue());
    assertThat("ack time was null", actualAck.getAckTime(), notNullValue());
    assertThat("ack time was not before ack was captured", preAckTime.before(actualAck.getAckTime()), equalTo(true));
    assertThat("uuid did not match", actualAck.getMessageUuid(), equalTo(expectedUuid));
    assertThat("source queue name did not match", actualAck.getSourceQueueName(), equalTo(expectedQueueName));
  }

  @Test
  public void messageCaptureTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();
    
    AckDivertPlugin plugin = new AckDivertPlugin(continuityCtx.getService());
    serverCtx.getServer().getConfiguration().registerBrokerPlugin(plugin);
 
    AckDivert divertMock = mock(AckDivert.class);
    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    MessageHandler handlerMock = mock(MessageHandler.class);

    String addressName = "async-sample1";
    String expectedQueueName = "async-sample1";
    String expectedUuid = UUID.randomUUID().toString();
    String expectedMessage = "test message";
    Date preAckTime = new Date(System.currentTimeMillis());

    when(flowMock.getAckDivert()).thenReturn(divertMock);
    when(continuityCtx.getService().locateFlow(expectedQueueName)).thenReturn(flowMock);
    when(continuityCtx.getService().isSubjectQueue(any(Queue.class))).thenReturn(true);

    // Send a message - expecting the message will still be sent, and the ack details will be captured by the ack-divert
    produceAndConsumeMessage(continuityCtx.getConfig(), serverCtx, addressName, expectedQueueName, handlerMock, expectedMessage, expectedUuid);
    
    ArgumentCaptor<ClientMessage> msgCaptor = ArgumentCaptor.forClass(ClientMessage.class);
    verify(handlerMock, times(1)).onMessage(msgCaptor.capture());
    ClientMessage receivedMessage = msgCaptor.getValue();
    assertThat("message was not received", receivedMessage.getBodyBuffer().readString(), equalTo(expectedMessage));

    ArgumentCaptor<AckInfo> ackCaptor = ArgumentCaptor.forClass(AckInfo.class);
    verify(divertMock).sendAck(ackCaptor.capture());
    AckInfo actualAck = ackCaptor.getValue();
    
    assertThat("ack was null", actualAck, notNullValue());
    assertThat("msg send time was null", actualAck.getMessageSendTime(), notNullValue());
    assertThat("ack time was null", actualAck.getAckTime(), notNullValue());
    assertThat("ack time was not before ack was captured", preAckTime.before(actualAck.getAckTime()), equalTo(true));
    assertThat("uuid did not match", actualAck.getMessageUuid(), equalTo(expectedUuid));
    assertThat("source queue name did not match", actualAck.getSourceQueueName(), equalTo(expectedQueueName));
  }
  
  @Test
  public void messageCaptureWithDuplicateIdPluginTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();
    
    AckDivertPlugin plugin = new AckDivertPlugin(continuityCtx.getService());
    serverCtx.getServer().getConfiguration().registerBrokerPlugin(plugin);

    DuplicateIdPlugin dupIdPlugin = new DuplicateIdPlugin(continuityCtx.getService());
    serverCtx.getServer().getConfiguration().registerBrokerPlugin(dupIdPlugin);
 
    AckDivert divertMock = mock(AckDivert.class);
    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    MessageHandler handlerMock = mock(MessageHandler.class);

    String addressName = "async-sample1";
    String expectedQueueName = "async-sample1";
    String expectedMessage = "test message";
    Date preAckTime = new Date(System.currentTimeMillis());

    when(continuityCtx.getService().isSubjectAddress(addressName)).thenReturn(true);
    
    when(flowMock.getAckDivert()).thenReturn(divertMock);
    when(continuityCtx.getService().locateFlow(expectedQueueName)).thenReturn(flowMock);
    when(continuityCtx.getService().isSubjectQueue(any(Queue.class))).thenReturn(true);

    // Send a message - expecting the message will still be sent, and the ack details will be captured by the ack-divert
    produceAndConsumeMessage(continuityCtx.getConfig(), serverCtx, addressName, expectedQueueName, handlerMock, expectedMessage, null);
 
    ArgumentCaptor<ClientMessage> msgCaptor = ArgumentCaptor.forClass(ClientMessage.class);
    verify(handlerMock, times(1)).onMessage(msgCaptor.capture());
    ClientMessage receivedMessage = msgCaptor.getValue();
    assertThat("message was not received", receivedMessage.getBodyBuffer().readString(), equalTo(expectedMessage));

    ArgumentCaptor<AckInfo> ackCaptor = ArgumentCaptor.forClass(AckInfo.class);
    verify(divertMock).sendAck(ackCaptor.capture());
    AckInfo actualAck = ackCaptor.getValue();
    
    assertThat("ack was null", actualAck, notNullValue());
    assertThat("msg send time was null", actualAck.getMessageSendTime(), notNullValue());
    assertThat("ack time was null", actualAck.getAckTime(), notNullValue());
    assertThat("ack time was not before ack was captured", preAckTime.before(actualAck.getAckTime()), equalTo(true));
    assertThat("uuid was null but expected duplicate id plugin to add", actualAck.getMessageUuid(), notNullValue());
    assertThat("source queue name did not match", actualAck.getSourceQueueName(), equalTo(expectedQueueName));
  }
}
