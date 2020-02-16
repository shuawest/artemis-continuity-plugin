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
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.impl.AckReason;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.core.AckInterceptor;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckInterceptorPluginTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(AckInterceptorPluginTest.class);

  @Test
  public void mockedMessageTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
 
    AckInterceptor interceptorMock = mock(AckInterceptor.class);
    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    Queue queueMock = mock(Queue.class);
    MessageReference refMock = mock(MessageReference.class);
    Message msgMock = mock(Message.class);
    AckReason reasonMock = mock(AckReason.class);

    String expectedQueueName = "async-sample1";
    String expectedUuid = UUID.randomUUID().toString();

    when(flowMock.getAckInterceptor()).thenReturn(interceptorMock);
    when(continuityCtx.getService().locateFlow(expectedQueueName)).thenReturn(flowMock);
    when(continuityCtx.getService().isSubjectQueue(any(Queue.class))).thenReturn(true);
    when(queueMock.isDurable()).thenReturn(true);
    when(queueMock.isTemporary()).thenReturn(false);
    when(queueMock.getName()).thenReturn(SimpleString.toSimpleString(expectedQueueName));
    when(refMock.getQueue()).thenReturn(queueMock);
    when(refMock.getMessage()).thenReturn(msgMock);
    when(msgMock.getStringProperty(Message.HDR_DUPLICATE_DETECTION_ID)).thenReturn(expectedUuid);

    AckInterceptorPlugin plugin = new AckInterceptorPlugin(continuityCtx.getService());
    plugin.messageAcknowledged(refMock, reasonMock, null);

    ArgumentCaptor<MessageReference> refCaptor = ArgumentCaptor.forClass(MessageReference.class);
    ArgumentCaptor<AckReason> reasonCaptor = ArgumentCaptor.forClass(AckReason.class);
    verify(interceptorMock, times(1)).handleMessageAcknowledgement(refCaptor.capture(), reasonCaptor.capture());
    MessageReference actualRef = refCaptor.getValue();
    AckReason actualReason = reasonCaptor.getValue();
    
    assertThat("msgRef was null", actualRef, notNullValue());
    assertThat("msgRef was null", actualReason, notNullValue());
  }

  // @Test
  // public void messageCaptureWithDuplicateIdPluginTest() throws Exception {
  //   ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
  //   ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
  //   serverCtx.getServer().start();
    
  //   AckInterceptorPlugin plugin = new AckInterceptorPlugin(continuityCtx.getService());
  //   serverCtx.getServer().getConfiguration().registerBrokerPlugin(plugin);

  //   DuplicateIdPlugin dupIdPlugin = new DuplicateIdPlugin(continuityCtx.getService());
  //   serverCtx.getServer().getConfiguration().registerBrokerPlugin(dupIdPlugin);
 
  //   ContinuityFlow flowMock = mock(ContinuityFlow.class);
  //   MessageHandler handlerMock = mock(MessageHandler.class);
  //   Queue queueMock = mock(Queue.class);
  //   MessageReference refMock = mock(MessageReference.class);
  //   Message msgMock = mock(Message.class);

  //   AckInterceptor ackInterceptor = new AckInterceptor(continuityCtx.getService(), flowMock);
  //   when(flowMock.getAckInterceptor()).thenReturn(ackInterceptor);
  //   ackInterceptor.start();

  //   String addressName = "async-sample1";
  //   String outflowAcksName = "async-sample1.out.acks.mock";
  //   String expectedQueueName = "async-sample1";
  //   String expectedMessage = "test message";
  //   String expectedUuid = UUID.randomUUID().toString();
  //   Date preAckTime = new Date(System.currentTimeMillis());

  //   when(continuityCtx.getService().isSubjectAddress(addressName)).thenReturn(true); 
  //   when(flowMock.getAckInterceptor()).thenReturn(ackInterceptor);
  //   when(flowMock.getSubjectAddressName()).thenReturn(addressName);
  //   when(flowMock.getSubjectQueueName()).thenReturn(addressName);
  //   when(flowMock.getOutflowAcksName()).thenReturn(outflowAcksName);
  //   when(continuityCtx.getService().locateFlow(expectedQueueName)).thenReturn(flowMock);
  //   when(continuityCtx.getService().isSubjectQueue(any(Queue.class))).thenReturn(true);

  //   when(continuityCtx.getService().locateFlow(expectedQueueName)).thenReturn(flowMock);
  //   when(continuityCtx.getService().isSubjectQueue(any(Queue.class))).thenReturn(true);
  //   when(queueMock.isDurable()).thenReturn(true);
  //   when(queueMock.isTemporary()).thenReturn(false);
  //   when(queueMock.getName()).thenReturn(SimpleString.toSimpleString(expectedQueueName));
  //   when(refMock.getQueue()).thenReturn(queueMock);
  //   when(refMock.getMessage()).thenReturn(msgMock);
  //   when(msgMock.getStringProperty(Message.HDR_DUPLICATE_DETECTION_ID)).thenReturn(expectedUuid);

  //   // Send a message - expecting the message will still be sent, and the ack details will be captured by the ack-divert
  //   produceAndConsumeMessage(continuityCtx.getConfig(), serverCtx, addressName, expectedQueueName, handlerMock, expectedMessage, null);
 
  //   Queue outflowAcksQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(outflowAcksName));
  //   assertThat("out acks queue has no message", outflowAcksQueue.browserIterator().hasNext(), equalTo(true));
    
  //   MessageReference msgRef = outflowAcksQueue.browserIterator().next();
  //   assertThat("ack info message was null", msgRef, notNullValue());
    
  //   String receivedBody = msgRef.getMessage().toCore().getBodyBuffer().readString();
  //   assertThat("ack info body is null", receivedBody, notNullValue());

  //   AckInfo actualAck = AckInfo.fromJSON(receivedBody);  
  //   assertThat("ack was null", actualAck, notNullValue());
  //   assertThat("msg send time was null", actualAck.getMessageSendTime(), notNullValue());
  //   assertThat("ack time was null", actualAck.getAckTime(), notNullValue());
  //   assertThat("ack time was not before ack was captured", preAckTime.before(actualAck.getAckTime()), equalTo(true));
  //   assertThat("uuid was null but expected duplicate id plugin to add", actualAck.getMessageUuid(), notNullValue());
  //   assertThat("source queue name did not match", actualAck.getSourceQueueName(), equalTo(expectedQueueName));
  // }
}
