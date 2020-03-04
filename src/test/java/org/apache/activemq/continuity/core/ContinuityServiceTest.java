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
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.plugins.ContinuityPlugin;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityServiceTest extends ContinuityTestBase {

  private static final Logger log = LoggerFactory.getLogger(ContinuityServiceTest.class);

  @Test
  public void constructorTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "ContinuityServiceTest.constructorTest",
        "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    when(continuityCtx.getConfig().getOutflowMirrorSuffix()).thenReturn(".out.mirror");
    when(continuityCtx.getConfig().getOutflowAcksSuffix()).thenReturn(".out.acks");
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getConfig().getInflowAcksSuffix()).thenReturn(".in.acks");

    ContinuityService svc = new ContinuityService(serverCtx.getServer(), continuityCtx.getConfig());

    assertThat("ContinuitySevice server", svc.getServer(), notNullValue());
    assertThat("ContinuitySevice config", svc.getConfig(), notNullValue());

    assertThat("ContinuitySevice isSubjectAddress", svc.isSubjectAddress("example1"), equalTo(true));
    assertThat("ContinuitySevice isSubjectAddress", svc.isSubjectAddress("example2"), equalTo(true));
    assertThat("ContinuitySevice isSubjectAddress", svc.isSubjectAddress("example2.out.mirror"), equalTo(false));

    assertThat("ContinuitySevice isSubjectQueue", svc.isSubjectQueue("example1-durable"), equalTo(true));
    assertThat("ContinuitySevice isSubjectQueue", svc.isSubjectQueue("example1-nondurable"), equalTo(false));

    assertThat("ContinuitySevice isOutflowMirrorAddress", svc.isOutflowMirrorAddress("example1.out.mirror"), equalTo(true));
    assertThat("ContinuitySevice isOutflowMirrorAddress", svc.isOutflowMirrorAddress("example1.out.mirror.asdf"), equalTo(false));

    assertThat("ContinuitySevice isOutflowAcksAddress", svc.isOutflowAcksAddress("example1.out.acks"), equalTo(true));
    assertThat("ContinuitySevice isOutflowAcksAddress", svc.isOutflowAcksAddress("example1.out.acks.asdf"), equalTo(false));

    assertThat("ContinuitySevice isInflowMirrorAddress", svc.isInflowMirrorAddress("example1.in.mirror"), equalTo(true));
    assertThat("ContinuitySevice isInflowMirrorAddress", svc.isInflowMirrorAddress("example1.in.mirror.asdf"), equalTo(false));

    assertThat("ContinuitySevice isInflowAcksAddress", svc.isInflowAcksAddress("example1.in.acks"), equalTo(true));
    assertThat("ContinuitySevice isInflowAcksAddress", svc.isInflowAcksAddress("example1.in.acks.asdf"), equalTo(false));

    CommandManager cmdMgrMock = mock(CommandManager.class);
    svc.registerCommandManager(cmdMgrMock);
    assertThat(svc.getCommandManager(), equalTo(cmdMgrMock));

    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    svc.registerContinuityFlow("example1", flowMock);
    assertThat(svc.locateFlow("example1"), equalTo(flowMock));
    assertThat(svc.locateFlow("asdf"), nullValue());

    serverCtx.getServer().asyncStop(() -> {
    });
  }

  @Test
  public void initializeTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "ContinuityServiceTest.initializeTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    when(continuityCtx.getConfig().getServingAcceptors()).thenReturn(Arrays.asList("artemis"));
    when(continuityCtx.getConfig().getOutflowAcksSuffix()).thenReturn(".out.acks");
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getConfig().getInflowAcksSuffix()).thenReturn(".in.acks");

    ContinuityService svc = new ContinuityService(serverCtx.getServer(), continuityCtx.getConfig());
    svc.initialize();

    assertThat(svc.getCommandManager(), notNullValue());

    serverCtx.getServer().asyncStop(() -> {
    });
  }

  @Test
  public void handleAddQueueTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "ContinuityServiceTest.handleAddQueueTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    when(continuityCtx.getCommandManager().isStarted()).thenReturn(true);
    when(continuityCtx.getConfig().getServingAcceptors()).thenReturn(Arrays.asList("artemis"));
    when(continuityCtx.getConfig().getOutflowMirrorSuffix()).thenReturn(".out.mirror");
    when(continuityCtx.getConfig().getOutflowAcksSuffix()).thenReturn(".out.acks");
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getConfig().getInflowAcksSuffix()).thenReturn(".in.acks");
    when(continuityCtx.getConfig().getLocalConnectorRef()).thenReturn("local-connector");

    ContinuityService svc = new ContinuityService(serverCtx.getServer(), continuityCtx.getConfig());
    svc.registerCommandManager(continuityCtx.getCommandManager());

    Queue queue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString("async-sample1"));

    svc.handleAddQueue(queue);

    assertThat(svc.locateFlow("async-sample1"), notNullValue());
    verify(continuityCtx.getCommandManager(), times(1)).sendCommand(any(ContinuityCommand.class));

    serverCtx.getServer().asyncStop(() -> {});
  }

  @Test
  public void pauseAcceptorsTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-with-plugin.xml", "ContinuityServiceTest.pauseAcceptorsTest", "myuser", "mypass");
    ServerContext serverCtx2 = createServerContext("broker2-with-plugin.xml", "ContinuityServiceTest.pauseAcceptorsTest", "myuser", "mypass");
    serverCtx1.getServer().start();
    serverCtx2.getServer().start();
    Thread.sleep(2000L);

    ContinuityPlugin plugin1 = getContinuityPlugin(serverCtx1);
    Queue example1Queue = serverCtx1.getServer().locateQueue(SimpleString.toSimpleString("example1-durable"));

    ScheduledProducerExecutor producerExecutor = startScheduledProducer(serverCtx1.getServer(), "tcp://0.0.0.0:61616", "myuser", "mypass", "example1", 25L);
    ScheduledConsumerExecutor consumerExecutor = startScheduledConsumer(serverCtx1.getServer(), "tcp://0.0.0.0:61616", "myuser", "mypass", "example1-durable", 50L);

    Thread.sleep(500L);
    
    long addedCountBefore = example1Queue.getMessagesAdded();
    long ackedCountBefore = example1Queue.getMessagesAcknowledged();

    // stop new connections from being accepted
    plugin1.getService().stopServingAcceptors();
    // kill existing connections on non-continuity acceptors
    plugin1.getService().stopServingDelivery();

    Thread.sleep(1000L);

    long addedCountAfter = example1Queue.getMessagesAdded();
    long ackedCountAfter = example1Queue.getMessagesAcknowledged();
    long producedCount = producerExecutor.getProducedCount();
    long consumedCount = consumerExecutor.getConsumedCount();
    
    log.debug("addedCountBefore '{}' addedCountAfter '{}', producedCount '{}'", addedCountBefore, addedCountAfter, producerExecutor.getProducedCount());
    log.debug("ackedCountBefore '{}' ackedCountAfter '{}', consumedCount '{}'", ackedCountBefore, ackedCountAfter, consumerExecutor.getConsumedCount());
    
    assertThat(producedCount, equalTo(addedCountAfter));
    assertThat(consumedCount, equalTo(ackedCountAfter));

    producerExecutor.stop();
    consumerExecutor.stop();
    serverCtx1.getServer().asyncStop(() -> {});
  }



}
