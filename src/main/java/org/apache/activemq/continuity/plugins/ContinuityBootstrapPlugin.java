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

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.apache.activemq.continuity.core.ContinuityBootstrapService;
import org.apache.activemq.continuity.core.ContinuityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityBootstrapPlugin implements ActiveMQServerPlugin {

  private static final Logger log = LoggerFactory.getLogger(ContinuityBootstrapPlugin.class);

  private ContinuityBootstrapService bootstrapService;

  @Override
  public void registered(ActiveMQServer server) {
    log.debug("Creating continuity bootstrap");
    try {
      this.bootstrapService = new ContinuityBootstrapService(server);
      bootstrapService.initialize();
    } catch (ContinuityException e) {
      log.error("Unable to initialize continuity bootstrap", e);
    }
  }

  public ContinuityBootstrapService getBootstrapService() {
    return bootstrapService;
  }
  
}
