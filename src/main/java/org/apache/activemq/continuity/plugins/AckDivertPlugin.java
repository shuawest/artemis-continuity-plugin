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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.ServerConsumer;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerMessagePlugin;
import org.apache.activemq.continuity.core.AckDivert;
import org.apache.activemq.continuity.core.AckInfo;
import org.apache.activemq.continuity.core.ContinuityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AckDivertPlugin implements ActiveMQServerMessagePlugin {

  private static final Logger log = LoggerFactory.getLogger(AckDivertPlugin.class);

  private final ContinuityService continuityService; 

  public AckDivertPlugin(final ContinuityService continuityService) {
    this.continuityService = continuityService;
  } 

  public void registered(ActiveMQServer server) {
    log.debug("AckDivertPlugin registered");
  }

  @Override
  public void afterDeliver(ServerConsumer consumer, MessageReference ref) throws ActiveMQException {
    Queue sourceQueue = ref.getQueue();
    String queueName = sourceQueue.getName().toString();

    if(continuityService.isSubjectQueue(sourceQueue)) {
      AckDivert ackDivert = continuityService.locateFlow(queueName).getAckDivert();

      Date msgTimestamp = new Date(ref.getMessage().getTimestamp());
      Date ackTime = new Date(System.currentTimeMillis());

      String dupId = ref.getMessage().getStringProperty(Message.HDR_DUPLICATE_DETECTION_ID);
      
      AckInfo ack = new AckInfo();
      ack.setMessageSendTime(msgTimestamp);
      ack.setAckTime(ackTime);
      ack.setMessageUuid(dupId);
      ack.setSourceQueueName(queueName);

      ackDivert.sendAck(ack);
    }
  }

}
