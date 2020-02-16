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

import java.util.UUID;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.RoutingContext;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerMessagePlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.continuity.core.ContinuityException;
import org.apache.activemq.continuity.core.ContinuityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DuplicateIdPlugin implements ActiveMQServerMessagePlugin {

  private static final Logger log = LoggerFactory.getLogger(DuplicateIdPlugin.class);

  private ContinuityService continuityService; 

  public DuplicateIdPlugin(ContinuityService continuityService) {
    this.continuityService = continuityService;
  } 

  public void registered(ActiveMQServer server) {
    log.debug("DuplicateIdPlugin registered");
  }

  @Override
  public void beforeSend(ServerSession session, Transaction tx, Message message, boolean direct, boolean noAutoCreateQueue) throws ContinuityException {
    if(log.isTraceEnabled()) {
      log.trace("before send address:'{}', dupProp:'{}', id:'{}', correlId:'{}'", message.getAddress(), message.getDuplicateProperty(), message.getMessageID(), message.getCorrelationID());
    }

    // Ensure there is a duplicate id UUID on messages for subject addresses 
    if(continuityService.isSubjectAddress(message.getAddress())) {

      // Only add duplicate id if it does not already have one
      if(message.getDuplicateIDBytes() == null) {
        String uuid = UUID.randomUUID().toString();
        message.putStringProperty(Message.HDR_DUPLICATE_DETECTION_ID, uuid);
        message.reencode();  

        if(log.isTraceEnabled()) {
          log.trace("Applied duplicate id to message on address '{}': {}", message.getAddress(), uuid);
        }
      }
    } 
  }
}
