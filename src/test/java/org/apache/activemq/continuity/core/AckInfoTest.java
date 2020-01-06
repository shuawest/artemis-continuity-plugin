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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.activemq.continuity.ContinuityTestBase;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckInfoTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(AckInfoTest.class);

  @Test
  public void toJson() throws Exception {
    AckInfo ack = new AckInfo();
    ack.setMessageSendTime(new Date(System.currentTimeMillis()-1000L));
    ack.setAckTime(new Date(System.currentTimeMillis()));
    ack.setMessageUuid(UUID.randomUUID().toString());
    ack.setSourceQueueName("async-sample1");

    String ackJson = AckInfo.toJSON(ack);
    log.debug("AckInfo json: {}", ackJson); 

    assertThat("ackinfo could not be marshaled to json", ackJson, notNullValue());
    assertThat("ackinfo could not be marshaled to json", ackJson.length(), greaterThan(0));
  }

  @Test
  public void fromJson() throws Exception {
    String ackJson = "{\"msg-send-time\":\"2020-01-02T15:40:01.309-0500\",\"ack-time\":\"2020-01-02T15:45:59.207-0500\",\"msg-uuid\":\"d49318e8-4fc3-2abc-4bc0-900351464057\",\"src-queue\":\"async-sample1\"}";

    AckInfo ack = AckInfo.fromJSON(ackJson);
    
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ");
    Date expectedMsgSendTime = dateFormat.parse("2020-01-02T15:40:01.309-0500");
    Date expectedAckTime = dateFormat.parse("2020-01-02T15:45:59.207-0500");
    
    assertThat("ackinfo could not marshaled from json", ack, notNullValue());
    assertThat("msg send time could not be unmarshaled from json", ack.getMessageSendTime(), equalTo(expectedMsgSendTime));
    assertThat("ack time could not be unmarshaled from json", ack.getAckTime(), equalTo(expectedAckTime));
    assertThat("message uuid could not be unmarshaled from json", ack.getMessageUuid(), equalTo("d49318e8-4fc3-2abc-4bc0-900351464057"));
    assertThat("source queue could not be unmarshaled from json", ack.getSourceQueueName(), equalTo("async-sample1"));
  }

  @Test
  public void toAndFromJson() throws Exception {
    AckInfo expectedAck = new AckInfo();
    expectedAck.setMessageSendTime(new Date(System.currentTimeMillis()-1000L));
    expectedAck.setAckTime(new Date(System.currentTimeMillis()));
    expectedAck.setMessageUuid(UUID.randomUUID().toString());
    expectedAck.setSourceQueueName("async-sample1");

    String ackJson = AckInfo.toJSON(expectedAck);
    AckInfo actualAck = AckInfo.fromJSON(ackJson);

    assertThat("ackinfo could not marshaled from json", actualAck, notNullValue());
    assertThat("msg send time could not be unmarshaled from json", actualAck.getMessageSendTime(), equalTo(expectedAck.getMessageSendTime()));
    assertThat("ack time could not be unmarshaled from json", actualAck.getAckTime(), equalTo(expectedAck.getAckTime()));
    assertThat("message uuid could not be unmarshaled from json", actualAck.getMessageUuid(), equalTo(expectedAck.getMessageUuid()));
    assertThat("source queue could not be unmarshaled from json", actualAck.getSourceQueueName(), equalTo(expectedAck.getSourceQueueName()));
  }

}
