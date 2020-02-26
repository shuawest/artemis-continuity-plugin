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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.cluster.Bridge;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.plugins.ContinuityPlugin;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InflowAcksConsumedWatcherTest extends ContinuityTestBase {

  private static final Logger log = LoggerFactory.getLogger(InflowAcksConsumedWatcherTest.class);

  @Test
  public void exhaustInfowAcksTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-with-plugin.xml", "InflowAcksConsumedWatcherTest.exhaustInfowAcksTest", "myuser", "mypass");
    ServerContext serverCtx2 = createServerContext("broker2-with-plugin.xml", "InflowAcksConsumedWatcherTest.exhaustInfowAcksTest", "myuser", "mypass");
    serverCtx1.getServer().start();
    serverCtx2.getServer().start();
    Thread.sleep(2000L);

    ContinuityPlugin plugin1 = getContinuityPlugin(serverCtx1);
    ContinuityPlugin plugin2 = getContinuityPlugin(serverCtx2);

    // Stop the ack recievers, until they are started later
    for (ContinuityFlow f : plugin2.getService().getFlows()) {
      f.getAckReceiver().stop();
    }

    produceMessages("vm://1", "myuser", "mypass", "example1", "test message", 100);
    CoreHandle core1 = startCoreConsumer("vm://1", "myuser", "mypass", "example1-durable", new CoreMessageHandlerStub("broker1"));
    Thread.sleep(1000L);

    // stop bridging mirror and acks from site1
    Collection<ContinuityFlow> flows = plugin1.getService().getFlows();
    for(ContinuityFlow flow : flows) {
      Bridge outflowMirrorBridge = flow.locateBridge(flow.getOutflowMirrorBridgeName());
      outflowMirrorBridge.stop();
      Bridge outflowAcksBridge = flow.locateBridge(flow.getOutflowAcksBridgeName());
      outflowAcksBridge.stop();
    }
  

    ContinuityService mockContinuityService = mock(ContinuityService.class);
    List<Queue> inflowAcksQueues = plugin2.getService().getInfowAcksQueues();
    when(mockContinuityService.getInfowAcksQueues()).thenReturn(inflowAcksQueues);

    InflowAcksConsumedWatcher watcher = 
      new InflowAcksConsumedWatcher(mockContinuityService, 100L,  
                                  serverCtx2.getServer().getScheduledPool(), 
                                  serverCtx2.getServer().getExecutorFactory().getExecutor());

    watcher.start();

    Thread.sleep(500L);

    ContinuityFlow flow = plugin2.getService().locateFlow("example1-durable");
    Queue infowAcksQueue = serverCtx2.getServer().locateQueue(SimpleString.toSimpleString(flow.getInflowAcksName()));
    
    assertThat(infowAcksQueue.getMessageCount(), greaterThan(0L));
    verify(mockContinuityService, times(0)).handleInflowQueuesConsumed();

    // Start the ack recievers, to ensure the watcher can detect 
    for (ContinuityFlow f : plugin2.getService().getFlows()) {
      f.getAckReceiver().start();
    }

    Thread.sleep(1000L);

    assertThat(infowAcksQueue.getMessageCount(), equalTo(0L));
    verify(mockContinuityService, times(1)).handleInflowQueuesConsumed();

    log.debug("\n\nStopping servers");
    core1.close();
    serverCtx1.getServer().asyncStop(()->{});
    serverCtx2.getServer().asyncStop(()->{});
  }

}
