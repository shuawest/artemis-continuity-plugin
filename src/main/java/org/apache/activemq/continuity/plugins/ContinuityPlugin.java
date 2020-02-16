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

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.apache.activemq.continuity.core.ContinuityConfig;
import org.apache.activemq.continuity.core.ContinuityService;
import org.apache.activemq.continuity.core.ServerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      log.debug("Creating continuity service");
      this.continuityService = new ContinuityService(server, continuityConfig);
      ServerListener.registerActivateCallback(server, continuityService);

      log.debug("Registering dependent plugins");
      Configuration brokerConfig = server.getConfiguration();
      brokerConfig.registerBrokerPlugin(new DestinationPlugin(continuityService));
      brokerConfig.registerBrokerPlugin(new DuplicateIdPlugin(continuityService));
      brokerConfig.registerBrokerPlugin(new InflowMirrorPlugin(continuityService));
      brokerConfig.registerBrokerPlugin(new AckDivertPlugin(continuityService));
  }

  public ContinuityService getService() {
    return continuityService;
  }

  public ContinuityConfig getConfig() {
    return continuityConfig;
  }
  
}
