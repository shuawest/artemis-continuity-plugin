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

public class ContinuityCommand {

  public static final String ACTION_ACTIVATE_SITE = "activate-site";
  public static final String ACTION_DEACTIVATE_SITE = "deactivate-site";
  public static final String ACTION_BROKER_CONNECT = "broker-connect";
  public static final String ACTION_ADD_ADDRESS = "add-address";
  public static final String ACTION_ADD_QUEUE = "add-queue";
  public static final String ACTION_REMOVE_ADDRESS = "remove-address";
  public static final String ACTION_REMOVE_QUEUE = "remove-queue";
  public static final String NOTIF_OUTFLOW_EXHAUSTED = "notif-outflow-exhausted";
  public static final String NOTIF_SITE_ACTIVE = "notif-site-active";
  public static final String NOTIF_SITE_INACTIVE = "notif-site-inactive";

  public static final String STATUS_ACTIVE = "active";
  public static final String STATUS_INACTIVE = "inacive";

  private static final String ACTION_FIELD = "action";
  private static final String ADDRESS_FIELD = "addr";
  private static final String QUEUE_FIELD = "queue";
  private static final String ROUTING_TYPE_FIELD = "routing";
  private static final String ORIGIN_FIELD = "origin";
  private static final String STATUS_FIELD = "status";

  

  private String action;
  private String address; 
  private String queue;
  private String routingType;
  private String origin;
  private String status;

  public ContinuityCommand() { }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getQueue() {
    return queue;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  public String getRoutingType() {
    return routingType;
  }

  public void setRoutingType(String routingType) {
    this.routingType = routingType;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  /** Marshalling **/

  public static String toJSON(ContinuityCommand cc) {
    JsonObjectBuilder builder = JsonLoader.createObjectBuilder();
    
    if(cc.getAction() != null)
      builder.add(ACTION_FIELD, cc.getAction());
    if(cc.getAddress() != null)
      builder.add(ADDRESS_FIELD, cc.getAddress());
    if(cc.getQueue() != null)
      builder.add(QUEUE_FIELD, cc.getQueue());
    if(cc.getRoutingType() != null)
      builder.add(ROUTING_TYPE_FIELD, cc.getRoutingType());
    if(cc.getOrigin() != null)
      builder.add(ORIGIN_FIELD, cc.getOrigin());
      if(cc.getStatus() != null)
      builder.add(STATUS_FIELD, cc.getStatus());

    JsonObject jsonObject = builder.build();
    return jsonObject.toString();
  }

  public static ContinuityCommand fromJSON(String json) throws ParseException {
    JsonObject jsonObject = JsonUtil.readJsonObject(json);
    ContinuityCommand cc = new ContinuityCommand();

    if(jsonObject.containsKey(ACTION_FIELD))
      cc.setAction(jsonObject.getString(ACTION_FIELD));
    if(jsonObject.containsKey(ADDRESS_FIELD))
      cc.setAddress(jsonObject.getString(ADDRESS_FIELD));
    if(jsonObject.containsKey(QUEUE_FIELD))
      cc.setQueue(jsonObject.getString(QUEUE_FIELD));
    if(jsonObject.containsKey(ROUTING_TYPE_FIELD))
      cc.setRoutingType(jsonObject.getString(ROUTING_TYPE_FIELD));
    if(jsonObject.containsKey(ORIGIN_FIELD))
      cc.setOrigin(jsonObject.getString(ORIGIN_FIELD));
      if(jsonObject.containsKey(STATUS_FIELD))
      cc.setStatus(jsonObject.getString(STATUS_FIELD));

    return cc;
  }


}
