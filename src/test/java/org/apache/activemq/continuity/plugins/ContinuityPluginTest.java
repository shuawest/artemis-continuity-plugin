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

import java.util.Arrays;
import java.util.HashMap;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityPluginTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(ContinuityPluginTest.class);

  @Test
  public void pluginStartTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker2-noplugin.xml", "ContinuityPluginTest.pluginStartTest", "myuser", "mypass");

    ContinuityPlugin plugin = new ContinuityPlugin();
    plugin.init(new HashMap<String, String>() {{
      put("site-id", "site2");
      put("local-username", "myuser");
      put("local-password", "mypass");
      put("remote-username", "myuser");
      put("remote-password", "mypass");
      put("serving-acceptors", "artemis");
      put("local-connector-ref", "local-connector");
      put("remote-connector-refs", "remote-connector");
      put("active-on-start", "true");
    }}); 

    serverCtx.getServer().getConfiguration().registerBrokerPlugin(plugin);
    serverCtx.getServer().start();
    Thread.sleep(700L);
    plugin.getService().activateSite(1L);
    Thread.sleep(300L);
    
    // create a dummy connection to start the plugin
    MessageHandlerStub dummyHandler = new MessageHandlerStub();
    CoreHandle conumerHandle = consumeDirect("tcp://localhost:61617", "myuser", "mypass", "artemis.continuity.commands.in", RoutingType.MULTICAST, "commandStubQueue", dummyHandler);

    assertThat(plugin.getConfig(), notNullValue());
    assertThat(plugin.getConfig().getSiteId(), equalTo("site2"));
    assertThat(plugin.getConfig().getLocalConnectorRef(), equalTo("local-connector"));
    assertThat(plugin.getConfig().getRemoteConnectorRefs(), equalTo(Arrays.asList("remote-connector")));
    assertThat(plugin.getConfig().getLocalUsername(), equalTo("myuser"));
    assertThat(plugin.getConfig().getLocalPassword(), equalTo("mypass"));
    assertThat(plugin.getConfig().getRemoteUsername(), equalTo("myuser"));
    assertThat(plugin.getConfig().getRemotePassword(), equalTo("mypass"));

    assertThat(plugin.getService(), notNullValue());
    assertThat(plugin.getService().isInitialized(), equalTo(true));
    assertThat(plugin.getService().isStarted(), equalTo(true));
    assertThat(plugin.getService().getCommandManager(), notNullValue());
    assertThat(plugin.getService().getCommandManager().isInitialized(), equalTo(true));
    assertThat(plugin.getService().getCommandManager().isStarted(), equalTo(true));
    assertThat(plugin.getService().getCommandManager().getCommandReceiver(), notNullValue());

    ContinuityFlow flow1 = plugin.getService().locateFlow("async-sample1");
    assertThat(flow1, notNullValue());
    assertThat(flow1.isInitialized(), equalTo(true));
    assertThat(flow1.getAckInterceptor(), notNullValue());
    assertThat(flow1.getAckInterceptor().isStarted(), equalTo(true));
    assertThat(flow1.getAckReceiver(), notNullValue());
    assertThat(flow1.getAckReceiver().isStarted(), equalTo(true));
    assertThat(flow1.getAckManager(), notNullValue());

    // cleanup
    conumerHandle.close();
    plugin.getService().stop();
    serverCtx.getServer().asyncStop(()->{});
  }
  
  @Test
  public void brokerConnectTest() throws Exception { 
    ServerContext serverCtx1 = createServerContext("broker1-with-plugin.xml", "ContinuityPluginTest.brokerConnectTest", "myuser", "mypass");
    ServerContext serverCtx2 = createServerContext("broker2-with-plugin.xml", "ContinuityPluginTest.brokerConnectTest", "myuser", "mypass");
    serverCtx1.getServer().start();
    serverCtx2.getServer().start();
    Thread.sleep(500L);
  
    log.debug("\n\nServers started\n\n");

    ContinuityPlugin plugin1 = getContinuityPlugin(serverCtx1);
    ContinuityPlugin plugin2 = getContinuityPlugin(serverCtx2);
    assertThat(plugin1, notNullValue());
    assertThat(plugin2, notNullValue());

    ContinuityFlow flow2a = plugin2.getService().locateFlow("example1-durable");
    ContinuityFlow flow2b = plugin2.getService().locateFlow("example2-durable");
    assertThat(flow2a, notNullValue());
    assertThat(flow2b, notNullValue());

    ContinuityFlow flow1a = plugin1.getService().locateFlow("example1-durable");
    ContinuityFlow flow1b = plugin1.getService().locateFlow("example2-durable");
    assertThat(flow1a, notNullValue());
    assertThat(flow1b, notNullValue());

    plugin1.getService().stop();
    plugin2.getService().stop();
    serverCtx1.getServer().asyncStop(()->{});
    serverCtx2.getServer().asyncStop(()->{});
  }

}
