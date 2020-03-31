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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerBasePlugin;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.core.ContinuityBootstrapService;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityBootstrapPluginTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(ContinuityBootstrapPluginTest.class);
 
  @Test
  public void bootstrapTest() throws Exception { 
    ServerContext serverCtx1 = createServerContext("broker1-bootstrap.xml", "ContinuityBootstrapPluginTest.bootstrapTest", "myuser", "mypass");
    serverCtx1.getServer().start();
    Thread.sleep(500L);
    log.debug("Server started");

    ContinuityBootstrapPlugin bootstrapPlugin = getBootstrapPlugin(serverCtx1);
    assertThat(bootstrapPlugin, notNullValue());

    ContinuityBootstrapService bootstrapService = bootstrapPlugin.getBootstrapService();
    assertThat(bootstrapService, notNullValue());

    bootstrapService.configure("site1", true, "artemis", "local-connector", "remote-connector", true);
    bootstrapService.setSecrets("myuser", "mypass", "asdf", "zxcv");
    bootstrapService.tune(200000L, 1234L, 0.9D, 2000L, 990000L);
    bootstrapService.boot();

    Thread.sleep(500L);

    assertThat(bootstrapService.isBooted(), equalTo(true));

    ContinuityPlugin continuityPlugin = bootstrapService.getContinuityPlugin();
    assertThat(continuityPlugin, notNullValue());
    
    assertThat(continuityPlugin.getService(), notNullValue());
    
    ContinuityFlow flow1 = continuityPlugin.getService().locateFlow("example1");
    assertThat(flow1, notNullValue());

    continuityPlugin.getService().stop();
    serverCtx1.getServer().asyncStop(()->{});
  }

  @Test
  public void destroyTest() throws Exception { 
    ServerContext serverCtx1 = createServerContext("broker1-bootstrap.xml", "ContinuityBootstrapPluginTest.destroyTest", "myuser", "mypass");
    serverCtx1.getServer().start();
    Thread.sleep(500L);
    log.debug("Server started");

    ContinuityBootstrapPlugin bootstrapPlugin = getBootstrapPlugin(serverCtx1);
    ContinuityBootstrapService bootstrapService = bootstrapPlugin.getBootstrapService();
    bootstrapService.configure("site1", true, "artemis", "local-connector", "remote-connector", true);
    bootstrapService.setSecrets("myuser", "mypass", "asdf", "zxcv");
    bootstrapService.tune(200000L, 1234L, 0.9D, 2000L, 990000L);
    bootstrapService.boot();

    Thread.sleep(500L);

    assertThat(bootstrapService.isBooted(), equalTo(true));

    bootstrapService.destroy();

    serverCtx1.getServer().asyncStop(()->{});
  }


  @Test
  public void rebootTest() throws Exception { 
    ServerContext serverCtx1 = createServerContext("broker1-bootstrap.xml", "ContinuityBootstrapPluginTest.rebootTest", "myuser", "mypass");
    serverCtx1.getServer().start();
    Thread.sleep(500L);
    log.debug("Server started");

    ContinuityBootstrapPlugin bootstrapPlugin = getBootstrapPlugin(serverCtx1);
    ContinuityBootstrapService bootstrapService = bootstrapPlugin.getBootstrapService();
    bootstrapService.configure("site1", true, "artemis", "local-connector", "remote-connector", true);
    bootstrapService.setSecrets("myuser", "mypass", "asdf", "zxcv");
    bootstrapService.tune(200000L, 1234L, 0.9D, 2000L, 990000L);
    bootstrapService.boot();

    Thread.sleep(500L);

    assertThat(bootstrapService.isBooted(), equalTo(true));

    bootstrapService.reboot();

    // TODO: add updates to the tuning, and verify settings are changed

    serverCtx1.getServer().asyncStop(()->{});
  }

  public ContinuityBootstrapPlugin getBootstrapPlugin(ServerContext serverCtx) {
    ContinuityBootstrapPlugin plugin = null;
    for(ActiveMQServerBasePlugin p : serverCtx.getServer().getBrokerPlugins()) {
      if(p.getClass().equals(ContinuityBootstrapPlugin.class)) {
        plugin = (ContinuityBootstrapPlugin)p;
        break;
      }
    }
    return plugin;
  }
}
