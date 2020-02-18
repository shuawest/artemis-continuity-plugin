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
package org.apache.activemq.continuity.management;

import static org.hamcrest.Matchers.equalTo;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerBasePlugin;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.apache.activemq.continuity.core.ContinuityService;
import org.apache.activemq.continuity.plugins.ContinuityPlugin;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityMangementServiceTest extends ContinuityTestBase {

  private static final Logger log = LoggerFactory.getLogger(ContinuityMangementServiceTest.class);

  @Test
  public void managementRegisterTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-with-plugin.xml", "ContinuityMangementServiceTest.managementRegisterTest", "myuser", "mypass");
    serverCtx.getServer().start();
    Thread.sleep(2000L);

    ContinuityPlugin plugin = getContinuityPlugin(serverCtx);
    ContinuityService service = plugin.getService();
    ContinuityFlow flow = (ContinuityFlow) service.getFlows().toArray()[0];
    ContinuityManagementService cms = service.getManagement();

    String continuityPrefix = cms.getContinuityServicePrefix();
    String continuityFlowPrefix = cms.getContinuityFlowPrefix(service, flow);

    assertTrue(isRegistered(continuityPrefix));

    assertTrue(isRegistered("%s,subcomponent=addresses,address=\"%s\"", 
                                continuityPrefix, 
                                service.getCommandManager().getCommandInQueueName()));

    assertTrue(isRegistered(continuityFlowPrefix));

    assertTrue(isRegistered("%s,subsubcomponent=addresses,address=\"%s\"", 
                                continuityFlowPrefix, 
                                flow.getInflowAcksName()));

    assertTrue(isRegistered("%s,subsubcomponent=addresses,address=\"%s\",subsubsubcomponent=queues,routing-type=\"anycast\",queue=\"%s\"", 
                                continuityFlowPrefix, 
                                flow.getInflowMirrorName(),
                                flow.getInflowMirrorName()));   

    assertTrue(isRegistered("%s,subsubcomponent=bridges,bridge=\"%s\"", 
                                continuityFlowPrefix, 
                                flow.getOutflowMirrorBridgeName())); 

    assertTrue(isRegistered("%s,subsubcomponent=diverts,divert=\"%s\"", 
                                continuityFlowPrefix, 
                                flow.getOutflowDivertName()));     
                        
    service.stop();
    serverCtx.getServer().asyncStop(()->{});
  }

  @Test
  public void formatStatsTest() {
    Double msAvg = Double.valueOf("177157.150390625");
    Double secAvg = msAvg / 1000;
    Double minAvg = secAvg / 60;    
    String avgFormat = String.format("%.2f ms (%.3f secs, or %.3f mins)", msAvg, secAvg, minAvg);
    log.debug("Average: {}", avgFormat);
    assertThat(avgFormat, equalTo("177157.15 ms (177.157 secs, or 2.953 mins)"));

    Long msPeak = Long.valueOf("177222");
    Double secPeak = msPeak.doubleValue() / 1000;
    Double minPeak = secPeak / 60;    
    String peakFormat = String.format("%d ms (%.3f secs, or %.3f mins)", msPeak, secPeak, minPeak);
    log.debug("Peak: {}", peakFormat);
    assertThat(peakFormat, equalTo("177222 ms (177.222 secs, or 2.954 mins)"));
  }

  private boolean isRegistered(String pattern, Object... args) throws Exception {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    String name = String.format(pattern, args);
    log.debug("Checking if registered: {}", name);
    ObjectName objName = new ObjectName(name);
    return mBeanServer.isRegistered(objName);
  }

  private ContinuityPlugin getContinuityPlugin(ServerContext serverCtx) {
    ContinuityPlugin plugin = null;
    for(ActiveMQServerBasePlugin p : serverCtx.getServer().getBrokerPlugins()) {
      if(p.getClass().equals(ContinuityPlugin.class)) {
        plugin = (ContinuityPlugin)p;
        break;
      }
    }
    return plugin;
  }

}
