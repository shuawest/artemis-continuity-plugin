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

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerMessagePlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.continuity.core.ContinuityException;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.apache.activemq.continuity.core.ContinuityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InflowMirrorPlugin implements ActiveMQServerMessagePlugin {

  private static final Logger log = LoggerFactory.getLogger(InflowMirrorPlugin.class);

  private ContinuityService continuityService; 

  public InflowMirrorPlugin(ContinuityService continuityService) {
    this.continuityService = continuityService;
  } 

  public void registered(ActiveMQServer server) {
    log.debug("InflowMirrorPlugin registered");
  }

  // TODO: determine if exception bubbled up without consequence
  @Override
  public void beforeSend(ServerSession session, Transaction tx, Message message, boolean direct, boolean noAutoCreateQueue) throws ContinuityException {
    String addressName = message.getAddress();
    
    if(continuityService.isInflowMirrorAddress(addressName)) {
      String subjectQueueName = addressName.replaceFirst(continuityService.getConfig().getInflowMirrorSuffix(), "") ;
      
      if(log.isDebugEnabled())
        log.debug("Locating flow with name {}", subjectQueueName);

      ContinuityFlow flow = continuityService.locateFlow(subjectQueueName);  
      flow.getAckManager().handleInflowMirrorMessage(message);  
    } 
  }
}
