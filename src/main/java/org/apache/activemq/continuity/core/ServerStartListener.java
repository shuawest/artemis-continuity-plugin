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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.management.CoreNotificationType;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.management.Notification;
import org.apache.activemq.artemis.core.server.management.NotificationListener;
import org.apache.activemq.artemis.utils.collections.TypedProperties;
import org.apache.activemq.continuity.management.ContinuityManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStartListener implements NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ServerStartListener.class);

    private final ContinuityService service;

    public ServerStartListener(final ContinuityService service) {
        this.service = service;
    }

    @Override
    public void onNotification(Notification notification) {
        // Look for the in-vm acceptor associated with the continuity plugin to initialize
        if(notification.getType() == CoreNotificationType.ACCEPTOR_STARTED) {
            if(log.isDebugEnabled()) {
                log.debug("Recieved AcceptorStarted Notification: {}", notification);
            }

            String inVmAcceptorId = service.getConfig().getLocalInVmUri().substring(5);
            String factory = getValueAsString(notification.getProperties(), "factory");
            String id = getValueAsString(notification.getProperties(), "id");

            if(factory != null && factory.endsWith("InVMAcceptorFactory") && id != null && id.equals(inVmAcceptorId)) {
                if(log.isDebugEnabled()) {
                    log.debug("Notification signaled started start: {}", notification);
                }

                service.handleServerStart();

                // Remove this listener after the server is started
                service.getServer().getManagementService().removeNotificationListener(this);
            }
      }
    }

    private String getValueAsString(TypedProperties props, String key) {
        SimpleString ssKey = SimpleString.toSimpleString(key);
        if(!props.containsProperty(ssKey)) {
            return null;
        } else {
            Object value = props.getProperty(ssKey);
            return (value == null)? null : value.toString();
        }
    }
}
