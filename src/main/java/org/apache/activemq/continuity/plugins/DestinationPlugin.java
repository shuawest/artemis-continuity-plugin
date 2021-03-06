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

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.security.SecurityAuth;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.apache.activemq.continuity.core.ContinuityException;
import org.apache.activemq.continuity.core.ContinuityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DestinationPlugin implements ActiveMQServerPlugin {

  private static final Logger log = LoggerFactory.getLogger(DestinationPlugin.class);

  private ContinuityService continuityService;

  public DestinationPlugin(ContinuityService continuityService) {
    this.continuityService = continuityService;
  } 

  public void registered(ActiveMQServer server) {
    log.debug("DestinationPlugin registered");
  }

  @Override
  public void afterCreateQueue(Queue queue) throws ActiveMQException {
    try {
      continuityService.handleAddQueue(queue);
    } catch(ContinuityException e) {
      log.error("Unable to handle queue creation", e);
    }
  }

  @Override
  public void afterDestroyQueue(Queue queue, SimpleString address, final SecurityAuth session, boolean checkConsumerCount,
                                  boolean removeConsumers, boolean autoDeleteAddress) throws ActiveMQException {    
    try {
      continuityService.handleRemoveQueue(queue); 
    } catch(ContinuityException e) {
      log.error("Unable to handle queue removal", e);
    }
  }
}
