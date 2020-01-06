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
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.activemq.artemis.api.core.JsonUtil;
import org.apache.activemq.artemis.utils.JsonLoader;

public class ContinuityCommand {

  public static final String ACTION_ADD_ADDRESS = "add-address";
  public static final String ACTION_ADD_QUEUE = "add-queue";
  public static final String ACTION_REMOVE_ADDRESS = "remove-address";
  public static final String ACTION_REMOVE_QUEUE = "remove-queue";

  private static final String UUID_FIELD = "uuid";
  private static final String ACTION_FIELD = "action";
  private static final String ADDRESS_FIELD = "addr";
  private static final String QUEUE_FIELD = "queue";

  private String uuid; 
  private String action;
  private String address; 
  private String queue;

  public static ContinuityCommand createCommand() {
    ContinuityCommand cmd = new ContinuityCommand();
    cmd.setUuid(UUID.randomUUID().toString());
    return cmd;
  }

  public ContinuityCommand() { }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

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

  /** Marshalling **/

  public static String toJSON(ContinuityCommand cc) {
    JsonObjectBuilder builder = JsonLoader.createObjectBuilder();
    builder.add(UUID_FIELD, cc.getUuid());
    builder.add(ACTION_FIELD, cc.getAction());
    builder.add(ADDRESS_FIELD, cc.getAddress());
    builder.add(QUEUE_FIELD, cc.getQueue());
    JsonObject jsonObject = builder.build();
    return jsonObject.toString();
  }

  public static ContinuityCommand fromJSON(String json) throws ParseException {
    JsonObject jsonObject = JsonUtil.readJsonObject(json);
    ContinuityCommand cc = new ContinuityCommand();
    cc.setUuid(jsonObject.getString(UUID_FIELD));
    cc.setAction(jsonObject.getString(ACTION_FIELD));
    cc.setAddress(jsonObject.getString(ADDRESS_FIELD));
    cc.setQueue(jsonObject.getString(QUEUE_FIELD));
    return cc;
  }

}
