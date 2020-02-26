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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.cluster.Bridge;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.plugins.ContinuityPlugin;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutflowExhaustedWatcherTest extends ContinuityTestBase {

  private static final Logger log = LoggerFactory.getLogger(OutflowExhaustedWatcherTest.class);

  @Test
  public void exhaustOutflowTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-with-plugin.xml", "OutflowExhaustedWatcherTest.exhaustOutflowTest", "myuser", "mypass");
    ServerContext serverCtx2 = createServerContext("broker2-with-plugin.xml", "OutflowExhaustedWatcherTest.exhaustOutflowTest", "myuser", "mypass");
    serverCtx1.getServer().start();
    serverCtx2.getServer().start();
    Thread.sleep(3000L);

    ContinuityPlugin plugin1 = getContinuityPlugin(serverCtx1);

    Collection<ContinuityFlow> flows = plugin1.getService().getFlows();
    for(ContinuityFlow flow : flows) {
      Bridge outflowMirrorBridge = flow.locateBridge(flow.getOutflowMirrorBridgeName());
      outflowMirrorBridge.stop();
      Bridge outflowAcksBridge = flow.locateBridge(flow.getOutflowAcksBridgeName());
      outflowAcksBridge.stop();
    }

    produceMessages("vm://1", "myuser", "mypass", "example1", "test message", 100);
    CoreHandle core1 = startCoreConsumer("vm://1", "myuser", "mypass", "example1-durable", new CoreMessageHandlerStub("broker1"));
    Thread.sleep(1000L);

    ContinuityService mockContinuityService = mock(ContinuityService.class);
    List<Queue> outflowQueues = plugin1.getService().getOutflowQueues();
    when(mockContinuityService.getOutflowQueues()).thenReturn(outflowQueues);

    OutflowExhaustedWatcher outflowWatcher = 
      new OutflowExhaustedWatcher(mockContinuityService, 100L,  
                                  serverCtx1.getServer().getScheduledPool(), 
                                  serverCtx1.getServer().getExecutorFactory().getExecutor());

    outflowWatcher.start();

    for(ContinuityFlow flow : flows) {
      Bridge outflowMirrorBridge = flow.locateBridge(flow.getOutflowMirrorBridgeName());
      outflowMirrorBridge.start();
      Bridge outflowAcksBridge = flow.locateBridge(flow.getOutflowAcksBridgeName());
      outflowAcksBridge.start();
    }

    Thread.sleep(1000L);

    verify(mockContinuityService, times(1)).handleOutflowExhausted();

    log.debug("\n\nStopping servers");
    core1.close();
    serverCtx1.getServer().asyncStop(()->{});
    serverCtx2.getServer().asyncStop(()->{});
  }

}
