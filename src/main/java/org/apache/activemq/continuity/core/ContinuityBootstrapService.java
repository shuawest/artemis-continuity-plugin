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

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.continuity.management.ContinuityManagementService;
import org.apache.activemq.continuity.plugins.ContinuityPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityBootstrapService {

  private static final Logger log = LoggerFactory.getLogger(ContinuityBootstrapService.class);

  private final ActiveMQServer server;
  private final ContinuityManagementService mgmt; 

  private ContinuityPlugin continuityPlugin;
  private Map<String, String> properties = new HashMap<String, String>();
  private boolean isBooted;

  public ContinuityBootstrapService(final ActiveMQServer server) {
    this.server = server;
    this.mgmt = new ContinuityManagementService(server.getManagementService());
  }

  public synchronized void initialize() throws ContinuityException {
    if (log.isInfoEnabled()) {
      log.info("Initializing continuity bootstrap");
    }

    mgmt.registerContinuityBootstrap(this);
  }

  public Boolean isBooted() {
    return isBooted;
  }

  public void boot() throws Exception {
    this.continuityPlugin = new ContinuityPlugin();
    continuityPlugin.init(properties);

    Configuration brokerConfig = server.getConfiguration();
    brokerConfig.registerBrokerPlugin(continuityPlugin);

    if(server.isStarted()) {
      continuityPlugin.registered(server);
      continuityPlugin.getService().initialize();
      continuityPlugin.getService().start();
    }

    isBooted = true;
  }

  public void reboot() throws Exception {
    if(isBooted)
      destroy();

    boot();
  }

  public void destroy() throws Exception {
    Configuration brokerConfig = server.getConfiguration();
    brokerConfig.unRegisterBrokerPlugin(continuityPlugin);
    isBooted = false;
  }

  public void configure(String siteId, 
                        Boolean activeOnStart, 
                        String servingAcceptors, 
                        String localConnectorRef,
                        String remoteConnectorRefs, 
                        Boolean reorgManagement) throws Exception 
  {
    properties.put(ContinuityConfig.CONFIG_SITE_ID, siteId);
    properties.put(ContinuityConfig.CONFIG_ACTIVE_ON_START, Boolean.toString(activeOnStart));
    properties.put(ContinuityConfig.CONFIG_SERVING_ACCEPTORS, servingAcceptors);
    properties.put(ContinuityConfig.CONFIG_LOCAL_CONNECTOR_REF, localConnectorRef);
    properties.put(ContinuityConfig.CONFIG_REMOTE_CONNECTOR_REFS, remoteConnectorRefs);
    properties.put(ContinuityConfig.CONFIG_REORG_MGMT, Boolean.toString(reorgManagement));
  }

  public void setSecrets(String localContinuityUser, 
                         String localContinuityPass, 
                         String remoteContinuityUser, 
                         String remoteontinuityPass) throws Exception 
  {
    properties.put(ContinuityConfig.CONFIG_LOCAL_USERNAME, localContinuityUser);
    properties.put(ContinuityConfig.CONFIG_LOCAL_PASSWORD, localContinuityPass);
    properties.put(ContinuityConfig.CONFIG_REMOTE_USERNAME, remoteContinuityUser);
    properties.put(ContinuityConfig.CONFIG_REMOTE_PASSWORD, remoteontinuityPass);
  }

  public void tune(Long activationTimeout,
                   Long inflowStagingDelay, 
                   Long bridgeInterval,
                   Double bridgeIntervalMultiplier, 
                   Long pollDuration) throws Exception 
  {
    if(activationTimeout != null)
      properties.put(ContinuityConfig.CONFIG_ACTIVATION_TIMEOUT, Long.toString(activationTimeout));
    
    if(inflowStagingDelay != null)
      properties.put(ContinuityConfig.CONFIG_INFLOW_STAGING_DELAY, Long.toString(inflowStagingDelay));

    if(bridgeInterval != null)
      properties.put(ContinuityConfig.CONFIG_BRIDGE_INTERVAL, Long.toString(bridgeInterval));

    if(bridgeIntervalMultiplier != null)
      properties.put(ContinuityConfig.CONFIG_BRIDGE_INTERVAL_MULTIPLIER, Double.toString(bridgeIntervalMultiplier));
      
    if(pollDuration != null) {
      properties.put(ContinuityConfig.CONFIG_OUTFLOW_EXHAUSTED_POLL_DURATION, Long.toString(pollDuration));
      properties.put(ContinuityConfig.CONFIG_INFLOW_ACKS_CONSUMED_POLL_DURATION, Long.toString(pollDuration));
    }
  } 

  public ContinuityPlugin getContinuityPlugin() {
    return continuityPlugin;
  }

  public ActiveMQServer getServer() {
    return server;
  }

}
