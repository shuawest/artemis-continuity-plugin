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

import java.util.HashMap;

import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityPluginTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(ContinuityPluginTest.class);

  @Test
  public void pluginStartTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker2-noplugin.xml", "test-broker2", "myuser", "mypass");

    ContinuityPlugin plugin = new ContinuityPlugin();
    plugin.init(new HashMap<String, String>() {{
      put("site-id", "site2");
      put("local-invm-uri", "vm://2");
      put("local-username", "myuser");
      put("local-password", "mypass");
      put("remote-connector-ref", "remote-connector");
      put("addresses", "async-sample1;async-sample2");
    }}); 

    serverCtx.getServer().getConfiguration().registerBrokerPlugin(plugin);
    serverCtx.getServer().start();
    Thread.sleep(1000L);

    //produceMessage(plugin.getConfig(), serverCtx, "async-sample1", "async-sample1", "test message", null);

    assertThat(plugin.getConfig(), notNullValue());
    assertThat(plugin.getConfig().getSiteId(), equalTo("site2"));
    assertThat(plugin.getConfig().getLocalInVmUri(), equalTo("vm://2"));
    assertThat(plugin.getConfig().getLocalUsername(), equalTo("myuser"));
    assertThat(plugin.getConfig().getLocalPassword(), equalTo("mypass"));
    assertThat(plugin.getConfig().getAddresses().size(), equalTo(2));
    assertThat(plugin.getConfig().getAddresses().get(0), equalTo("async-sample1"));
    assertThat(plugin.getConfig().getAddresses().get(1), equalTo("async-sample2"));

    assertThat(plugin.getService(), notNullValue());
    assertThat(plugin.getService().isInitialized(), equalTo(true));
    assertThat(plugin.getService().getCommandManager(), notNullValue());
    assertThat(plugin.getService().getCommandManager().isInitialized(), equalTo(true));
    assertThat(plugin.getService().getCommandManager().getCommandHandler(), notNullValue());

    assertThat(plugin.getService().getFlows().size(), equalTo(1));
    ContinuityFlow flow1 = plugin.getService().locateFlow("async-sample1");
    assertThat(flow1, notNullValue());
    assertThat(flow1.isInitialized(), equalTo(true));
    assertThat(flow1.getAckDivert(), notNullValue());
    assertThat(flow1.getAckDivert().isInitialized(), equalTo(true));
    assertThat(flow1.getAckReceiver(), notNullValue());
    assertThat(flow1.getAckReceiver().isInitialized(), equalTo(true));
    assertThat(flow1.getAckManager(), notNullValue());
  }
  
}
