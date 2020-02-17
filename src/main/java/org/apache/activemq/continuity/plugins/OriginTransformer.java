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

import java.util.Map;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OriginTransformer implements Transformer {

  private static final Logger log = LoggerFactory.getLogger(OriginTransformer.class);

  public static final String ORIGIN_HEADER_NAME = "_CTY_ORIGIN";
  public static final String ORIGIN_CONFIG = "origin";

  private String messageOrigin;

  public String getMessageOrigin() {
    return messageOrigin;
  }

  public void setMessageOrigin(String messageOrigin) {
    this.messageOrigin = messageOrigin;
  }

  @Override
  public void init(Map<String, String> properties) {
    this.setMessageOrigin(properties.get(ORIGIN_CONFIG));
  }

  @Override
  public Message transform(Message msg) {
    msg.putStringProperty(ORIGIN_HEADER_NAME, messageOrigin);
    return msg;
  }
}
