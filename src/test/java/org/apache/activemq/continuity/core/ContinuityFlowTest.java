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

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.cluster.Bridge;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityFlowTest extends ContinuityTestBase {

  private static final Logger log = LoggerFactory.getLogger(ContinuityFlowTest.class);

  @Test
  public void constructorTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);

    when(continuityCtx.getConfig().getOutflowMirrorSuffix()).thenReturn(".out.mirror");
    when(continuityCtx.getConfig().getOutflowAcksSuffix()).thenReturn(".out.acks");
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getConfig().getInflowAcksSuffix()).thenReturn(".in.acks");

    String subjectAddressName = "async-sample1";
    String subjectQueueName = "async-sample1-a";

    QueueInfo queueInfo = new QueueInfo();
    queueInfo.setAddressName(subjectAddressName);
    queueInfo.setQueueName(subjectQueueName);

    ContinuityFlow flow = new ContinuityFlow(continuityCtx.getService(), queueInfo);
    
    assertThat(flow.getSubjectAddressName(), equalTo(subjectAddressName));
    assertThat(flow.getSubjectQueueName(), equalTo(subjectQueueName));
    assertThat(flow.getOutflowMirrorName(), endsWith(".out.mirror"));
    assertThat(flow.getOutflowMirrorBridgeName(), endsWith(".out.mirror.bridge"));
    assertThat(flow.getOutflowAcksName(), endsWith(".out.acks"));
    assertThat(flow.getOutflowAcksBridgeName(), endsWith(".out.acks.bridge"));
    assertThat(flow.getInflowMirrorName(), endsWith(".in.mirror"));
    assertThat(flow.getInflowAcksName(), endsWith(".in.acks"));
    assertThat(flow.getTargetBridgeName(), endsWith(".in.mirror.bridge"));

    assertThat(flow.getOutflowMirrorName(), equalTo("async-sample1-a.out.mirror"));
    assertThat(flow.getOutflowMirrorBridgeName(), equalTo("async-sample1-a.out.mirror.bridge"));
    assertThat(flow.getOutflowAcksName(), equalTo("async-sample1-a.out.acks"));
    assertThat(flow.getOutflowAcksBridgeName(), equalTo("async-sample1-a.out.acks.bridge"));
    assertThat(flow.getInflowMirrorName(), equalTo("async-sample1-a.in.mirror"));
    assertThat(flow.getInflowAcksName(), equalTo("async-sample1-a.in.acks"));
    assertThat(flow.getTargetBridgeName(), equalTo("async-sample1-a.in.mirror.bridge"));
  }

  @Test
  public void initializeTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    when(continuityCtx.getConfig().getOutflowMirrorSuffix()).thenReturn(".out.mirror");
    when(continuityCtx.getConfig().getOutflowAcksSuffix()).thenReturn(".out.acks");
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getConfig().getInflowAcksSuffix()).thenReturn(".in.acks");
    when(continuityCtx.getConfig().getLocalConnectorRef()).thenReturn("local-connector");

    String subjectAddressName = "async-sample1";
    String subjectQueueName = "async-sample1";

    QueueInfo queueInfo = new QueueInfo();
    queueInfo.setAddressName(subjectAddressName);
    queueInfo.setQueueName(subjectQueueName);

    ContinuityFlow flow = new ContinuityFlow(continuityCtx.getService(), queueInfo);
    flow.initialize();
    flow.start();
    Thread.sleep(300L);
    
    verify(continuityCtx.getService(), times(1)).registerContinuityFlow(eq(subjectQueueName), eq(flow));

    verifyQueueExists(serverCtx, flow.getSubjectQueueName(), RoutingType.ANYCAST);
    verifyQueueExists(serverCtx, flow.getOutflowMirrorName(), RoutingType.MULTICAST);
    verifyBridgeExistsInState(serverCtx, flow.getOutflowMirrorBridgeName(), flow.getOutflowMirrorName(), flow.getInflowMirrorName(), true);
    verifyQueueExists(serverCtx, flow.getOutflowAcksName(), RoutingType.MULTICAST);
    verifyBridgeExistsInState(serverCtx, flow.getOutflowAcksBridgeName(), flow.getOutflowAcksName(), flow.getInflowAcksName(), true);
    verifyQueueExists(serverCtx, flow.getInflowMirrorName(), RoutingType.MULTICAST);
    verifyQueueExists(serverCtx, flow.getInflowAcksName(), RoutingType.MULTICAST);
    verifyBridgeExistsInState(serverCtx, flow.getTargetBridgeName(), flow.getInflowMirrorName(), flow.getSubjectAddressName(), false);

    assertThat("AckDivert", flow.getAckDivert(), notNullValue());
    assertThat("AckManager", flow.getAckManager(), notNullValue());
    assertThat("AckReceiver", flow.getAckReceiver(), notNullValue());
  }

  private void verifyQueueExists(ServerContext serverCtx, String qName, RoutingType routingType) {
    Queue q = serverCtx.getServer().locateQueue(SimpleString.toSimpleString(qName));
    assertThat("queue does not exist: " + qName, q, notNullValue());
    assertThat("queue has wrong routing type: " + qName, q.getRoutingType(), equalTo(routingType));
  }

  private void verifyBridgeExistsInState(ServerContext serverCtx, String bridgeName, String sourceName, String targetName, boolean isStarted) {
    Bridge bridge = serverCtx.getServer().getClusterManager().getBridges().get(bridgeName);
    assertThat("bridge does not exist: " + bridgeName, notNullValue());
    assertThat("bridge source queue name is wrong: " + bridgeName, bridge.getQueue().getName().toString(), equalTo(sourceName));
    assertThat("bridge forwarding address is wrong: " + bridgeName, bridge.getForwardingAddress().toString(), equalTo(targetName));
    assertThat("bridge is not in expected state: " + bridgeName, bridge.isStarted(), equalTo(isStarted));
  }

}
