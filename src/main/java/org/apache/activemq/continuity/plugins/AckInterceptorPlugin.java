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
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.ServerConsumer;
import org.apache.activemq.artemis.core.server.impl.AckReason;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerMessagePlugin;
import org.apache.activemq.continuity.core.AckInterceptor;
import org.apache.activemq.continuity.core.ContinuityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AckInterceptorPlugin implements ActiveMQServerMessagePlugin {

  private static final Logger log = LoggerFactory.getLogger(AckInterceptorPlugin.class);

  private final ContinuityService continuityService; 

  public AckInterceptorPlugin(final ContinuityService continuityService) {
    this.continuityService = continuityService;
  } 

  public void registered(ActiveMQServer server) {
    log.debug("AckInterceptorPlugin registered");
  }

  @Override
  public void messageAcknowledged(MessageReference ref, AckReason reason, ServerConsumer consumer) throws ActiveMQException {
    Queue sourceQueue = ref.getQueue();
    String queueName = sourceQueue.getName().toString();

    if(continuityService.isSubjectQueue(sourceQueue)) {
      AckInterceptor ackInterceptor = continuityService.locateFlow(queueName).getAckInterceptor();
      ackInterceptor.handleMessageAcknowledgement(ref, reason);
    }
  }

}
