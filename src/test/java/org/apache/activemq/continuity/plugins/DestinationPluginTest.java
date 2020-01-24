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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DestinationPluginTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(DestinationPluginTest.class);

  @Test
  public void addQueueTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker2-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    
    DestinationPlugin plugin = new DestinationPlugin(continuityCtx.getService());
    serverCtx.getServer().getConfiguration().registerBrokerPlugin(plugin);

    serverCtx.getServer().start();

    verify(continuityCtx.getService(), times(3)).handleAddQueue(any(Queue.class));
  }
  
}
