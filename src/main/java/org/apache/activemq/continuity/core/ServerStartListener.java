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

import org.apache.activemq.artemis.core.server.ActivateCallback;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStartListener implements ActivateCallback {

    private static final Logger log = LoggerFactory.getLogger(ServerStartListener.class);

    private final ContinuityService service;

    public static void registerActivateCallback(ActiveMQServer server, ContinuityService service) {
        ServerStartListener listener = new ServerStartListener(service);
        server.registerActivateCallback(listener);
    }

    
    public ServerStartListener(final ContinuityService service) {
        this.service = service;
    }

    @Override
    public void preActivate() {
        if(log.isDebugEnabled()) {
            log.debug("Server preActivate");
        }
    }

    @Override
    public void activated() { 
        if(log.isDebugEnabled()) {
            log.debug("Server activated");
        }

        try {
            service.initialize();
        } catch(ContinuityException e) {
            log.error("Unable to initialize continuity service", e);
        }
    }

    @Override
    public void activationComplete() {
        if(log.isDebugEnabled()) {
            log.debug("Server activationComplete");
        }

        try {
            service.start();
        } catch(ContinuityException e) {
            log.error("Unable to start continuity service", e);
        }
    }
}
