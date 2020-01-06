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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.plugins.InflowMirrorPlugin;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckManagerTest extends ContinuityTestBase {

  private static final Logger log = LoggerFactory.getLogger(AckManagerTest.class);

  @Test
  public void messageRemovedFromMirrorTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    String expectedSubjectQueueName = "async-sample1";
    String expectedInflowQueueName = "async-sample1.in.mirror.mock";
    String expectedUuid = UUID.randomUUID().toString();

    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    when(flowMock.getSubjectQueueName()).thenReturn(expectedSubjectQueueName);
    when(flowMock.getInflowMirrorName()).thenReturn(expectedInflowQueueName);

    produceMessage(continuityCtx, serverCtx, expectedInflowQueueName, expectedInflowQueueName, "test message 1", UUID.randomUUID().toString());
    produceMessage(continuityCtx, serverCtx, expectedInflowQueueName, expectedInflowQueueName, "test message 2", expectedUuid);
    produceMessage(continuityCtx, serverCtx, expectedInflowQueueName, expectedInflowQueueName, "test message 3", UUID.randomUUID().toString());

    Queue inflowMirrorQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(expectedInflowQueueName));
    long preHandleQueueCount = inflowMirrorQueue.getDurableMessageCount();

    AckManager ackManager = new AckManager(continuityCtx.getService(), flowMock);
    ackManager.removeMessageFromMirror(expectedInflowQueueName, expectedUuid);

    long postHandleQueueCount = inflowMirrorQueue.getDurableMessageCount();

    assertThat("message was not removed", postHandleQueueCount, equalTo(preHandleQueueCount - 1));
  }

  @Test
  public void duplicateIdCacheTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    String expectedSubjectQueueName = "async-sample1";
    String expectedInflowQueueName = "async-sample1.in.mirror.mock";
    String expectedUuid = UUID.randomUUID().toString();

    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    when(flowMock.getSubjectQueueName()).thenReturn(expectedSubjectQueueName);
    when(flowMock.getInflowMirrorName()).thenReturn(expectedInflowQueueName);

    AckManager ackManager = new AckManager(continuityCtx.getService(), flowMock);
    ackManager.addDuplicateIdToTarget(expectedUuid);

    produceMessage(continuityCtx, serverCtx, expectedSubjectQueueName, expectedSubjectQueueName, "test message 1", UUID.randomUUID().toString());
    produceMessage(continuityCtx, serverCtx, expectedSubjectQueueName, expectedSubjectQueueName, "test message 2", expectedUuid);
    produceMessage(continuityCtx, serverCtx, expectedSubjectQueueName, expectedSubjectQueueName, "test message 3", UUID.randomUUID().toString());

    if(log.isDebugEnabled())
      ackManager.printDupIdCache(expectedSubjectQueueName);

    Queue subjectQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(expectedSubjectQueueName));
    long postHandleQueueCount = subjectQueue.getDurableMessageCount();

    assertThat("message was not removed due to duplicate id in cache", postHandleQueueCount, equalTo(2L));
  }

  @Test
  public void delayMessageOnInflowWithPluginTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    String subjectQueueName = "async-sample1.mock";
    String inflowMirrorQueueName = "async-sample1.in.mirror.mock";
    long expectedDelay = 2000L; // 2 second delay

    ContinuityFlow flowMock = mock(ContinuityFlow.class);

    AckManager ackManager = new AckManager(continuityCtx.getService(), flowMock);
    InflowMirrorPlugin inflowMirrorPlugin = new InflowMirrorPlugin(continuityCtx.getService());
    serverCtx.getServer().getConfiguration().registerBrokerPlugin(inflowMirrorPlugin);

    when(flowMock.getAckManager()).thenReturn(ackManager);
    when(flowMock.getSubjectQueueName()).thenReturn(subjectQueueName);
    when(flowMock.getInflowMirrorName()).thenReturn(inflowMirrorQueueName);
    when(continuityCtx.getConfig().getInflowStagingDelay()).thenReturn(expectedDelay);
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getService().isInflowMirrorAddress(inflowMirrorQueueName)).thenReturn(true);
    when(continuityCtx.getService().locateFlow(subjectQueueName)).thenReturn(flowMock);
    
    long preSendTime = System.currentTimeMillis();
    produceMessage(continuityCtx, serverCtx, inflowMirrorQueueName, inflowMirrorQueueName, "test message 1", UUID.randomUUID().toString());
    produceMessage(continuityCtx, serverCtx, inflowMirrorQueueName, inflowMirrorQueueName, "test message 2", UUID.randomUUID().toString());
    produceMessage(continuityCtx, serverCtx, inflowMirrorQueueName, inflowMirrorQueueName, "test message 3", UUID.randomUUID().toString());    

    MessageHandler handlerStub = new MessageHandlerStub();
    Queue inflowMirrorQueue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(inflowMirrorQueueName));

    long pollTimeout = preSendTime + 10000;
    Long pollExitTime = null;
    while(true) {
      if (inflowMirrorQueue.getDurableMessageCount() == 0) {
        pollExitTime = System.currentTimeMillis();
        break;
      }
      if (System.currentTimeMillis() >= pollTimeout) {
        break;
      }
      consumeMessages(continuityCtx, serverCtx, inflowMirrorQueueName, inflowMirrorQueueName, handlerStub);
      Thread.sleep(10L);
    }

    assertThat("messages not consumed after 10s timeout", pollExitTime, notNullValue());
    
    long actualConsumeDuration = pollExitTime - preSendTime;
    assertThat("messages consumed before delay", actualConsumeDuration, greaterThan(expectedDelay));
  }

  @Test
  public void ackStatsTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);

    ContinuityFlow flowMock = mock(ContinuityFlow.class);

    AckManager ackManager = new AckManager(continuityCtx.getService(), flowMock);
    ackManager.setAddDuplicatesToTarget(false);
    ackManager.setRemoveMessageFromMirror(false);
    
    long currentTime = System.currentTimeMillis();
    AckInfo ack1 = new AckInfo();
    ack1.setMessageSendTime(new Date(currentTime - 5000L));
    ack1.setAckTime(new Date(currentTime));
    ackManager.handleAck(ack1);
    assertThat("peak ack duration wrong on ack1", ackManager.getPeakAckDuration(), equalTo(5000L));
    assertThat("average ack duration wrong on ack1", ackManager.getAverageAckDuration(), equalTo(5000L));

    AckInfo ack2 = new AckInfo();
    ack2.setMessageSendTime(new Date(currentTime - 3000L));
    ack2.setAckTime(new Date(currentTime));
    ackManager.handleAck(ack2);
    assertThat("peak ack duration wrong on ack2", ackManager.getPeakAckDuration(), equalTo(5000L));
    assertThat("average ack duration wrong ack2", ackManager.getAverageAckDuration(), equalTo(4000L));

    AckInfo ack3 = new AckInfo();
    ack3.setMessageSendTime(new Date(currentTime - 8000L));
    ack3.setAckTime(new Date(currentTime));
    ackManager.handleAck(ack3);
    assertThat("peak ack duration wrong on ack3", ackManager.getPeakAckDuration(), equalTo(8000L));
    assertThat("average ack duration wrong ack3", ackManager.getAverageAckDuration(), equalTo(6000L));
  }
}
