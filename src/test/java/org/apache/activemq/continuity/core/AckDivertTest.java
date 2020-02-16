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
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.UUID;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckDivertTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(AckDivertTest.class);

  @Test
  public void initializeTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();
    
    String subjectAddressName = "async-sample1";
    String outflowAcksName = "async-sample1.out.acks.mock";

    ContinuityFlow flow = mock(ContinuityFlow.class);
    when(flow.getSubjectAddressName()).thenReturn(subjectAddressName);
    when(flow.getSubjectQueueName()).thenReturn(subjectAddressName);
    when(flow.getOutflowAcksName()).thenReturn(outflowAcksName);

    int connectionCount = serverCtx.getServer().getConnectionCount();

    AckDivert divert = new AckDivert(continuityCtx.getService(), flow);
    divert.start();

    assertThat("divert not started", divert.isStarted(), equalTo(true));
    
    Queue outflowAcksQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(outflowAcksName));
    assertThat("cannot find outflow acks queue after divert", outflowAcksQueue, notNullValue());
    assertThat("unexpected ack connection count queue after divert", serverCtx.getServer().getConnectionCount(), equalTo(connectionCount+1));

    divert.stop();
  }

  @Test
  public void sendAckBodyTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    String subjectAddressName = "async-sample1";
    String outflowAcksName = "async-sample1.out.acks.mock";

    ContinuityFlow flow = mock(ContinuityFlow.class);
    when(flow.getSubjectAddressName()).thenReturn(subjectAddressName);
    when(flow.getSubjectQueueName()).thenReturn(subjectAddressName);
    when(flow.getOutflowAcksName()).thenReturn(outflowAcksName);

    AckDivert divert = new AckDivert(continuityCtx.getService(), flow);
    divert.start();

    divert.sendAck("test message");
    Thread.sleep(10);
    
    Queue outflowAcksQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(outflowAcksName));
    assertThat("out acks queue has no message", outflowAcksQueue.browserIterator().hasNext(), equalTo(true));
    MessageReference msgRef = outflowAcksQueue.browserIterator().next();
    assertThat("message was null", msgRef, notNullValue());
    assertThat("message origin header is wrong", msgRef.getMessage().getStringProperty(AckDivert.ORIGIN_HEADER), equalTo(continuityCtx.getConfig().getSiteId()));
    assertThat("message body is wrong", msgRef.getMessage().toCore().getBodyBuffer().readString(), equalTo("test message"));
    
    divert.stop();
  }

  @Test
  public void sendAckInfoTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    String subjectAddressName = "async-sample1";
    String outflowAcksName = "async-sample1.out.acks.mock";

    ContinuityFlow flow = mock(ContinuityFlow.class);
    when(flow.getSubjectAddressName()).thenReturn(subjectAddressName);
    when(flow.getSubjectQueueName()).thenReturn(subjectAddressName);
    when(flow.getOutflowAcksName()).thenReturn(outflowAcksName);

    AckDivert divert = new AckDivert(continuityCtx.getService(), flow);
    divert.start();

    AckInfo ack = new AckInfo();
    ack.setMessageSendTime(new Date(System.currentTimeMillis() - 1000));
    ack.setAckTime(new Date(System.currentTimeMillis()));
    ack.setMessageUuid(UUID.randomUUID().toString());
    ack.setSourceQueueName("async-sample1");

    divert.sendAck(ack);
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
    
    divert.stop();
  }

}
