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
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.cluster.Bridge;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.plugins.ContinuityPlugin;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivationTimeoutExecutorTest extends ContinuityTestBase {

  private static final Logger log = LoggerFactory.getLogger(ActivationTimeoutExecutorTest.class);

  @Test
  public void timeoutExecutorTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-with-plugin.xml", "ActivationTimeoutExecutorTest.timeoutExecutorTest", "myuser", "mypass");
    serverCtx1.getServer().start();
    Thread.sleep(1000L);

    ContinuityService mockContinuityService = mock(ContinuityService.class);

    ActivationTimeoutExecutor executor = 
      new ActivationTimeoutExecutor(mockContinuityService, 2000L,
        serverCtx1.getServer().getScheduledPool(), 
        serverCtx1.getServer().getExecutorFactory().getExecutor());

    log.debug("Starting timeout executor");
    executor.start();
    Thread.sleep(500L);
    verify(mockContinuityService, times(0)).startSubjectQueueDelivery();

    Thread.sleep(4000L);
    verify(mockContinuityService, times(1)).startSubjectQueueDelivery();

    serverCtx1.getServer().asyncStop(()->{});
  }

}
