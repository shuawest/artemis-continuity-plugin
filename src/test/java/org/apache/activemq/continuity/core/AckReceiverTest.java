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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.UUID;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckReceiverTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(AckReceiverTest.class);

  @Test
  public void initializeTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "AckReceiverTest.initializeTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();
    
    String inflowAckName = "async-sample1.in.acks.mock";

    AckManager ackMgrMock = mock(AckManager.class);
    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    when(flowMock.getAckManager()).thenReturn(ackMgrMock);
    when(flowMock.getInflowAcksName()).thenReturn(inflowAckName);

    int connectionCount = serverCtx.getServer().getConnectionCount();

    AckReceiver ackReceiver = new AckReceiver(continuityCtx.getService(), flowMock);
    ackReceiver.start();

    assertThat("receiver not started", ackReceiver.isStarted(), equalTo(true));
    
    Queue outAcksQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(inflowAckName));
    assertThat("cannot find in acks queue after divert", outAcksQueue, notNullValue());
    assertThat("unexpected ack connection count queue after divert", serverCtx.getServer().getConnectionCount(), equalTo(connectionCount+1));

    ackReceiver.stop();
    serverCtx.getServer().asyncStop(()->{});
  }

  @Test
  public void receiveAckTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "AckReceiverTest.receiveAckTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();
   
    String inflowAckName = "async-sample1.in.acks.mock";

    AckManager ackMgrMock = mock(AckManager.class);
    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    when(flowMock.getAckManager()).thenReturn(ackMgrMock);
    when(flowMock.getInflowAcksName()).thenReturn(inflowAckName);

    AckInfo expectedAck = new AckInfo();
    expectedAck.setMessageSendTime(new Date(System.currentTimeMillis() - 1000));
    expectedAck.setAckTime(new Date(System.currentTimeMillis()));
    expectedAck.setMessageUuid(UUID.randomUUID().toString());
    expectedAck.setSourceQueueName("async-sample1");
    String expectedAckJson = AckInfo.toJSON(expectedAck);
    log.debug("Expected ack json: {}", expectedAckJson);

    AckReceiver ackReceiver = new AckReceiver(continuityCtx.getService(), flowMock);
    ackReceiver.start();

    // Send a ack over the inflow queue
    produceMessage(continuityCtx.getConfig(), serverCtx, inflowAckName, inflowAckName, expectedAckJson, null);
    Thread.sleep(500);
    
    ArgumentCaptor<AckInfo> ackCaptor = ArgumentCaptor.forClass(AckInfo.class);
    verify(ackMgrMock, times(1).description("ackManager.handleAck was not called")).handleAck(ackCaptor.capture());
    AckInfo actualAck = ackCaptor.getValue();

    assertThat("ack was null", actualAck, notNullValue());
    assertThat("ack time did not match", actualAck.getAckTime(), equalTo(expectedAck.getAckTime()));
    assertThat("uuid did not match", actualAck.getMessageUuid(), equalTo(expectedAck.getMessageUuid()));
    assertThat("source queue name did not match", actualAck.getSourceQueueName(), equalTo(expectedAck.getSourceQueueName()));

    ackReceiver.stop();
    serverCtx.getServer().asyncStop(()->{});
  }
  
}
