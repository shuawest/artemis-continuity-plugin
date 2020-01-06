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

import java.text.ParseException;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.activemq.artemis.api.core.JsonUtil;
import org.apache.activemq.artemis.utils.JsonLoader;

public class QueueInfo {

  private static final String ADDRESS_NAME_FIELD = "addr-name";
  private static final String QUEUE_NAME_FIELD = "queue-name";

  private String addressName;
  private String queueName;

  public QueueInfo() { }

  public String getAddressName() {
    return addressName;
  }

  public void setAddressName(String name) {
    this.addressName = name;
  }

  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String name) {
    this.queueName = name;
  }

  /** Marshalling **/

  public static String toJSON(QueueInfo qi) {
    JsonObjectBuilder builder = JsonLoader.createObjectBuilder();
    builder.add(ADDRESS_NAME_FIELD, qi.getAddressName());
    builder.add(QUEUE_NAME_FIELD, qi.getQueueName());
    JsonObject jsonObject = builder.build();
    return jsonObject.toString();
  }

  public static QueueInfo fromJSON(String json) throws ParseException {
    JsonObject jsonObject = JsonUtil.readJsonObject(json);
    QueueInfo qi = new QueueInfo();
    qi.setAddressName(jsonObject.getString(ADDRESS_NAME_FIELD));
    qi.setQueueName(jsonObject.getString(QUEUE_NAME_FIELD));
    return qi;
  }
}
