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

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler implements MessageHandler {

  private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

  private final ContinuityService service;

  public CommandHandler(final ContinuityService service) {
    this.service = service;
  }

  public void onMessage(ClientMessage message) {
    String body = message.getBodyBuffer().readString();

    if(log.isDebugEnabled()) {
      log.debug("Received command: {}", body);
    }

    try {
      ContinuityCommand command = ContinuityCommand.fromJSON(body);
      service.handleIncomingCommand(command);
    } catch (ParseException e) {
      String msg = String.format("Unable to parse ContinuityCommand: %s", body);
      log.error(msg, e);
    } catch(ContinuityException e) {
      String msg = String.format("Unable to handle ContinuityCommand: %s", body);
      log.error(msg, e);
      // TODO: send error to service to handle graceful failure of plugin
    }
  }

  private ContinuityConfig getConfig() {
    return service.getConfig();
  }

  private ActiveMQServer getServer() {
    return service.getServer();
  }

}
