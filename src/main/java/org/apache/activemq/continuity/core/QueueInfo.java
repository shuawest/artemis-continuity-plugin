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
  private static final String ROUTING_TYPE_FIELD = "routing-type";

  private String addressName;
  private String queueName;
  private String routingType;

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

  public String getRoutingType() {
    return routingType;
  }

  public void setRoutingType(String routingType) {
    this.routingType = routingType;
  }

  /** Marshalling **/

  public static String toJSON(QueueInfo qi) {
    JsonObjectBuilder builder = JsonLoader.createObjectBuilder();

    if(qi.getAddressName() != null)
      builder.add(ADDRESS_NAME_FIELD, qi.getAddressName());
    if(qi.getQueueName() != null)
      builder.add(QUEUE_NAME_FIELD, qi.getQueueName());
      if(qi.getRoutingType() != null)
      builder.add(ROUTING_TYPE_FIELD, qi.getRoutingType());

    JsonObject jsonObject = builder.build();
    return jsonObject.toString();
  }

  public static QueueInfo fromJSON(String json) throws ParseException {
    JsonObject jsonObject = JsonUtil.readJsonObject(json);
    QueueInfo qi = new QueueInfo();

    if(jsonObject.containsKey(ADDRESS_NAME_FIELD))
      qi.setAddressName(jsonObject.getString(ADDRESS_NAME_FIELD));
    if(jsonObject.containsKey(QUEUE_NAME_FIELD))
      qi.setQueueName(jsonObject.getString(QUEUE_NAME_FIELD));
    if(jsonObject.containsKey(ROUTING_TYPE_FIELD))
      qi.setRoutingType(jsonObject.getString(ROUTING_TYPE_FIELD));
      
    return qi;
  }

}
