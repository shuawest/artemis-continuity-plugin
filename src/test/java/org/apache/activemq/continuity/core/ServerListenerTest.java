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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerListenerTest extends ContinuityTestBase {

  private static final Logger log = LoggerFactory.getLogger(ServerListenerTest.class);

  @Test
  public void startTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "ServerListenerTest.startTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    
    when(continuityCtx.getConfig().getLocalInVmUri()).thenReturn("vm://1");

    ActiveMQServerPlugin stubPlugin =  new ActiveMQServerPlugin() {
      @Override
      public void registered(ActiveMQServer server) {
        ServerListener.registerActivateCallback(server, continuityCtx.getService());
      }
    };
    Configuration brokerConfig = serverCtx.getServer().getConfiguration();
    brokerConfig.registerBrokerPlugin(stubPlugin);

    serverCtx.getServer().start();

    verify(continuityCtx.getService(), times(1)).initialize();
    verify(continuityCtx.getService(), times(1)).start();
    
    serverCtx.getServer().asyncStop(()->{});
  }

}
