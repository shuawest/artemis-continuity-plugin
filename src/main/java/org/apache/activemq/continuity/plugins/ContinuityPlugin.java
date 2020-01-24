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

import java.util.Map;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.continuity.core.ContinuityConfig;
import org.apache.activemq.continuity.core.ContinuityException;
import org.apache.activemq.continuity.core.ContinuityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: finish core components and wiring
// TODO: create integration test for dual server failover
// TODO: test with shared nothing cluster
// TODO: test with active:passive cluster
// TODO: add mbeans - see ManagementServiceImpl.java.registerInJmx
// TODO: evaluate address & queue federation and how to use it instead of mirrors
// TODO: add continuity strategy config for delayed delivery, auto-adjusted delivery delay, zero delivery until activation, or ...
// TODO: adjust delivery delay based on stats
// TOOD: determine error handling strategy, capture messages and gracefully fail, or take down server
// TOOD: send command info across sites - addresses/queues, bridges?, diverts?, etc?
// TODO: deal with queue updates
// TOOD: deal with graceful teardown of continuity elements on server shutdown
// TODO: peformance test - measure impact of continuity
// TODO: create a k8s operator for managing failover
// TODO: create strong documentation
// TODO: create presentation to engineering
// TODO: update to contribute to ActiveMQ community
public class ContinuityPlugin implements ActiveMQServerPlugin {

  private static final Logger log = LoggerFactory.getLogger(ContinuityPlugin.class);

  private ContinuityService continuityService;
  private ContinuityConfig continuityConfig; 

  @Override
  public void init(Map<String, String> properties) {
    this.continuityConfig = new ContinuityConfig(properties);
  }

  @Override
  public void registered(ActiveMQServer server) {
    try {
      log.debug("Creating continuity service");
      this.continuityService = new ContinuityService(server, continuityConfig);
      continuityService.initialize();

      log.debug("Registering dependent plugins");
      Configuration brokerConfig = server.getConfiguration();
      brokerConfig.registerBrokerPlugin(new DestinationPlugin(continuityService));
      brokerConfig.registerBrokerPlugin(new DuplicateIdPlugin(continuityService));
      brokerConfig.registerBrokerPlugin(new InflowMirrorPlugin(continuityService));
      brokerConfig.registerBrokerPlugin(new AckDivertPlugin(continuityService));
    } catch (ContinuityException e) {
      log.error("Failed to initialize continuity plugin", e);
    }
  }

  // Command manager can't be started until the broker is running 
  @Override
  public void afterCreateConnection(RemotingConnection connection) throws ActiveMQException {
    log.debug("Initializing command manager", connection.getRemoteAddress());
    if(!continuityService.isStarted() && !continuityService.isStarting()) {
      continuityService.start();
    }
  }

  public ContinuityService getService() {
    return continuityService;
  }

  public ContinuityConfig getConfig() {
    return continuityConfig;
  }
  
}
