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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.UUID;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.impl.AckReason;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckInterceptorTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(AckInterceptorTest.class);

  @Test
  public void initializeTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "AckInterceptorTest.initializeTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();
    
    String subjectAddressName = "async-sample1";
    String outflowAcksName = "async-sample1.out.acks.mock";

    ContinuityFlow flow = mock(ContinuityFlow.class);
    when(flow.getSubjectAddressName()).thenReturn(subjectAddressName);
    when(flow.getSubjectQueueName()).thenReturn(subjectAddressName);
    when(flow.getOutflowAcksName()).thenReturn(outflowAcksName);

    int connectionCount = serverCtx.getServer().getConnectionCount();

    AckInterceptor ackInterceptor = new AckInterceptor(continuityCtx.getService(), flow);
    ackInterceptor.start();

    assertThat("interceptor not started", ackInterceptor.isStarted(), equalTo(true));
    
    Queue outflowAcksQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(outflowAcksName));
    assertThat("cannot find outflow acks queue after interceptor", outflowAcksQueue, notNullValue());
    assertThat("unexpected ack connection count queue after interceptor", serverCtx.getServer().getConnectionCount(), equalTo(connectionCount+1));

    ackInterceptor.stop();
  }

  @Test
  public void sendAckBodyTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "AckInterceptorTest.sendAckBodyTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    String subjectAddressName = "async-sample1";
    String outflowAcksName = "async-sample1.out.acks.mock";

    ContinuityFlow flow = mock(ContinuityFlow.class);
    when(flow.getSubjectAddressName()).thenReturn(subjectAddressName);
    when(flow.getSubjectQueueName()).thenReturn(subjectAddressName);
    when(flow.getOutflowAcksName()).thenReturn(outflowAcksName);

    AckInterceptor ackInterceptor = new AckInterceptor(continuityCtx.getService(), flow);
    ackInterceptor.start();

    ackInterceptor.sendAck("test message");
    Thread.sleep(10);
    
    Queue outflowAcksQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(outflowAcksName));
    assertThat("out acks queue has no message", outflowAcksQueue.browserIterator().hasNext(), equalTo(true));
    MessageReference msgRef = outflowAcksQueue.browserIterator().next();
    assertThat("message was null", msgRef, notNullValue());
    assertThat("message origin header is wrong", msgRef.getMessage().getStringProperty(AckInterceptor.ORIGIN_HEADER), equalTo(continuityCtx.getConfig().getSiteId()));
    assertThat("message body is wrong", msgRef.getMessage().toCore().getBodyBuffer().readString(), equalTo("test message"));
    
    ackInterceptor.stop();
    serverCtx.getServer().asyncStop(()->{});
  }

  @Test
  public void sendAckInfoTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "AckInterceptorTest.sendAckInfoTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    String subjectAddressName = "async-sample1";
    String outflowAcksName = "async-sample1.out.acks.mock";

    ContinuityFlow flow = mock(ContinuityFlow.class);
    when(flow.getSubjectAddressName()).thenReturn(subjectAddressName);
    when(flow.getSubjectQueueName()).thenReturn(subjectAddressName);
    when(flow.getOutflowAcksName()).thenReturn(outflowAcksName);

    AckInterceptor ackInterceptor = new AckInterceptor(continuityCtx.getService(), flow);
    ackInterceptor.start();

    AckInfo ack = new AckInfo();
    ack.setMessageSendTime(new Date(System.currentTimeMillis() - 1000));
    ack.setAckTime(new Date(System.currentTimeMillis()));
    ack.setMessageUuid(UUID.randomUUID().toString());
    ack.setSourceQueueName("async-sample1");

    ackInterceptor.sendAck(ack);
    Thread.sleep(10);
    
    Queue outflowAcksQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(outflowAcksName));
    assertThat("out acks queue has no message", outflowAcksQueue.browserIterator().hasNext(), equalTo(true));
    
    MessageReference msgRef = outflowAcksQueue.browserIterator().next();
    assertThat("ack info message was null", msgRef, notNullValue());
    
    String receivedBody = msgRef.getMessage().toCore().getBodyBuffer().readString();
    assertThat("ack info body is null", receivedBody, notNullValue());
    
    AckInfo receivedAck = AckInfo.fromJSON(receivedBody);
    assertThat("ack info could not be unmarshalled from json", receivedAck, notNullValue());
    assertThat("ack info date did not be unmarshalled from json", receivedAck.getAckTime(), equalTo(ack.getAckTime()));
    assertThat("ack info uuid did not be unmarshalled from json", receivedAck.getMessageUuid(), equalTo(ack.getMessageUuid()));
    assertThat("ack info source queue name did not be unmarshalled from json", receivedAck.getSourceQueueName(), equalTo(ack.getSourceQueueName()));
    
    ackInterceptor.stop();
    serverCtx.getServer().asyncStop(()->{});
  }

  @Test
  public void handleMessageAckTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "AckInterceptorTest.handleMessageAckTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    String subjectAddressName = "async-sample1";
    String outflowAcksName = "async-sample1.out.acks.mock";
    String expectedQueueName = "async-sample1";
    String expectedUuid = UUID.randomUUID().toString();
    Date preAckTime = new Date(System.currentTimeMillis());

    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    when(flowMock.getSubjectAddressName()).thenReturn(subjectAddressName);
    when(flowMock.getSubjectQueueName()).thenReturn(subjectAddressName);
    when(flowMock.getOutflowAcksName()).thenReturn(outflowAcksName);

    Queue queueMock = mock(Queue.class);
    MessageReference refMock = mock(MessageReference.class);
    Message msgMock = mock(Message.class);
    when(continuityCtx.getService().locateFlow(expectedQueueName)).thenReturn(flowMock);
    when(continuityCtx.getService().isSubjectQueue(any(Queue.class))).thenReturn(true);
    when(queueMock.isDurable()).thenReturn(true);
    when(queueMock.isTemporary()).thenReturn(false);
    when(queueMock.getName()).thenReturn(SimpleString.toSimpleString(expectedQueueName));
    when(refMock.getQueue()).thenReturn(queueMock);
    when(refMock.getMessage()).thenReturn(msgMock);
    when(msgMock.getStringProperty(Message.HDR_DUPLICATE_DETECTION_ID)).thenReturn(expectedUuid);

    AckInterceptor ackInterceptor = new AckInterceptor(continuityCtx.getService(), flowMock);
    when(flowMock.getAckInterceptor()).thenReturn(ackInterceptor);
    ackInterceptor.start();

    ackInterceptor.handleMessageAcknowledgement(refMock, AckReason.NORMAL);
    Thread.sleep(10);
    
    Queue outflowAcksQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(outflowAcksName));
    assertThat("out acks queue has no message", outflowAcksQueue.browserIterator().hasNext(), equalTo(true));
    
    MessageReference msgRef = outflowAcksQueue.browserIterator().next();
    assertThat("ack info message was null", msgRef, notNullValue());
    
    String receivedBody = msgRef.getMessage().toCore().getBodyBuffer().readString();
    assertThat("ack info body is null", receivedBody, notNullValue());

    AckInfo actualAck = AckInfo.fromJSON(receivedBody);
    assertThat("ack was null", actualAck, notNullValue());
    assertThat("msg send time was null", actualAck.getMessageSendTime(), notNullValue());
    assertThat("ack time was null", actualAck.getAckTime(), notNullValue());
    assertThat("ack time was not before ack was captured", preAckTime.before(actualAck.getAckTime()), equalTo(true));
    assertThat("uuid did not match", actualAck.getMessageUuid(), equalTo(expectedUuid));
    assertThat("source queue name did not match", actualAck.getSourceQueueName(), equalTo(expectedQueueName));

    ackInterceptor.stop();
    serverCtx.getServer().asyncStop(()->{});
  }

  // @Test
  // public void messageCaptureTest() throws Exception {
  //   ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "AckInterceptorTest.messageCaptureTest", "myuser", "mypass");
  //   ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
  //   serverCtx.getServer().start();
    
  //   AckInterceptorPlugin plugin = new AckInterceptorPlugin(continuityCtx.getService());
  //   serverCtx.getServer().getConfiguration().registerBrokerPlugin(plugin);
 
  //   AckInterceptor interceptorMock = mock(AckInterceptor.class);
  //   ContinuityFlow flowMock = mock(ContinuityFlow.class);
  //   MessageHandler handlerMock = mock(MessageHandler.class);

  //   String addressName = "async-sample1";
  //   String expectedQueueName = "async-sample1";
  //   String expectedUuid = UUID.randomUUID().toString();
  //   String expectedMessage = "test message";
  //   Date preAckTime = new Date(System.currentTimeMillis());

  //   when(flowMock.getAckInterceptor()).thenReturn(interceptorMock);
  //   when(continuityCtx.getService().locateFlow(expectedQueueName)).thenReturn(flowMock);
  //   when(continuityCtx.getService().isSubjectQueue(any(Queue.class))).thenReturn(true);

  //   // Send a message - expecting the message will still be sent, and the ack details will be captured by the ack-divert
  //   produceAndConsumeMessage(continuityCtx.getConfig(), serverCtx, addressName, expectedQueueName, handlerMock, expectedMessage, expectedUuid);
    
  //   ArgumentCaptor<ClientMessage> msgCaptor = ArgumentCaptor.forClass(ClientMessage.class);
  //   verify(handlerMock, times(1)).onMessage(msgCaptor.capture());
  //   ClientMessage receivedMessage = msgCaptor.getValue();
  //   assertThat("message was not received", receivedMessage.getBodyBuffer().readString(), equalTo(expectedMessage));

  //   ArgumentCaptor<AckInfo> ackCaptor = ArgumentCaptor.forClass(AckInfo.class);
  //   verify(interceptorMock).sendAck(ackCaptor.capture());
  //   AckInfo actualAck = ackCaptor.getValue();
    
  //   assertThat("ack was null", actualAck, notNullValue());
  //   assertThat("msg send time was null", actualAck.getMessageSendTime(), notNullValue());
  //   assertThat("ack time was null", actualAck.getAckTime(), notNullValue());
  //   assertThat("ack time was not before ack was captured", preAckTime.before(actualAck.getAckTime()), equalTo(true));
  //   assertThat("uuid did not match", actualAck.getMessageUuid(), equalTo(expectedUuid));
  //   assertThat("source queue name did not match", actualAck.getSourceQueueName(), equalTo(expectedQueueName));

  //   serverCtx.getServer().asyncStop(()->{});
  // }
  
 

}
