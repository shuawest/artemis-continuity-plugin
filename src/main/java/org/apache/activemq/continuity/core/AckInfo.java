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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.activemq.artemis.api.core.JsonUtil;
import org.apache.activemq.artemis.utils.JsonLoader;

public class AckInfo {

  private static final DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ"); 
  private static final String ACK_TIME_FIELD = "ack-time";
  private static final String MSG_SEND_TIME_FIELD = "msg-send-time";
  private static final String MSG_UUID_FIELD = "msg-uuid";
  private static final String SRC_QUEUE_FIELD = "src-queue";
  
  private String sourceQueueName;
  private String messageUuid;
  private Date msgSendTime;
  private Date ackTime;

  public AckInfo() { }

  public String getSourceQueueName() {
    return sourceQueueName;
  }

  public void setSourceQueueName(String sourceQueueName) {
    this.sourceQueueName = sourceQueueName;
  }

  public String getMessageUuid() {
    return messageUuid;
  }

  public void setMessageUuid(String messageUuid) {
    this.messageUuid = messageUuid;
  }

  public Date getMessageSendTime() {
    return msgSendTime;
  }

  public void setMessageSendTime(Date msgSendTime) {
    this.msgSendTime = msgSendTime;
  }

  public Date getAckTime() {
    return ackTime;
  }

  public void setAckTime(Date ackTime) {
    this.ackTime = ackTime;
  }

  /** Marshalling **/

  public static String toJSON(AckInfo ack) {
    JsonObjectBuilder builder = JsonLoader.createObjectBuilder();

    if(ack.getMessageSendTime() != null)
      builder.add(MSG_SEND_TIME_FIELD, DATETIME_FORMAT.format(ack.getMessageSendTime()));
      
    if(ack.getAckTime() != null)
      builder.add(ACK_TIME_FIELD, DATETIME_FORMAT.format(ack.getAckTime()));

    if(ack.getMessageUuid() != null)
      builder.add(MSG_UUID_FIELD, ack.getMessageUuid());

    if(ack.getSourceQueueName() != null)
      builder.add(SRC_QUEUE_FIELD, ack.getSourceQueueName());

    JsonObject jsonObject = builder.build();
    return jsonObject.toString();
  }

  public static AckInfo fromJSON(String json) throws ParseException {
    JsonObject jsonObject = JsonUtil.readJsonObject(json);

    AckInfo ack = new AckInfo();

    if(jsonObject.containsKey(MSG_SEND_TIME_FIELD)) {
      Date msgSendTime = DATETIME_FORMAT.parse(jsonObject.getString(MSG_SEND_TIME_FIELD));
      ack.setMessageSendTime(msgSendTime);
    }

    if(jsonObject.containsKey(ACK_TIME_FIELD)) {
      Date ackTime = DATETIME_FORMAT.parse(jsonObject.getString(ACK_TIME_FIELD));
      ack.setAckTime(ackTime);
    }
    
    if(jsonObject.containsKey(MSG_UUID_FIELD)) 
      ack.setMessageUuid(jsonObject.getString(MSG_UUID_FIELD));
    
    if(jsonObject.containsKey(SRC_QUEUE_FIELD)) 
      ack.setSourceQueueName(jsonObject.getString(SRC_QUEUE_FIELD));

    return ack;
  }

}
