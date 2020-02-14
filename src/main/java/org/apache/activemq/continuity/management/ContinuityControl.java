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
package org.apache.activemq.continuity.management;

import javax.management.MBeanOperationInfo;

import org.apache.activemq.artemis.api.core.management.Attribute;
import org.apache.activemq.artemis.api.core.management.Operation;
import org.apache.activemq.artemis.api.core.management.Parameter;

public interface ContinuityControl {

    /* Status */

    @Attribute(desc = "Status: whether the continuity service is initialized")
    String isServiceInitialized();

    @Attribute(desc = "Status: whether the continuity service listener, producer, bridge primitives are started")
    String isServiceStarted();

    @Attribute(desc = "Status: whether the continuity command manager is initialized")
    String isCommandManagerInitialized();

    @Attribute(desc = "Status: whether the continuity command manager is started")
    String isCommandManagerStarted();

    /* Statistics */

    /* Volatile Configuration */

     /* Static Configuration */

    @Attribute(desc = "Config: unique name for the site cluster")
    String getSiteId();

    @Attribute(desc = "Config: local in-vm uri")    
    String getLocalInVmUri();
 
    @Attribute(desc = "Config: local username")   
    String getLocalUsername();
 
    @Attribute(desc = "Config: Bridge reconnect interval applied to the all continuity bridges")
    String getBridgeInterval();
    
    @Attribute(desc = "Config: Bridge reconnect backoff multiplier applied to the all continuity bridges")
    String getBridgeIntervalMultiplier();
    
    @Attribute(desc = "Config: Name of addresses subject to continuity")
    String[] getAddresses();

    @Attribute(desc = "Config: Suffix added to the generated outflow mirror queues")   
    String getOutflowMirrorSuffix();

    @Attribute(desc = "Config: Suffix added to the generated outflow acks queues")    
    String getOutflowAcksSuffix();

    @Attribute(desc = "Config: Suffix added to the generated inflow mirror queues")    
    String getInflowMirrorSuffix();
    
    @Attribute(desc = "Config: Suffix added to the generated inflow ack queues")
    String getInflowAcksSuffix();
    
    @Attribute(desc = "Config: Staging duration for delivery of messages to the target queue for removal of acknowledged message removal")
    String getInflowStagingDelay();
    
    @Attribute(desc = "Config: Command destination name prefix")
    String getCommandDestinationPrefix();
    
    @Attribute(desc = "Config: Name of the local connector used for continuity")
    String getLocalConnector();
    
    @Attribute(desc = "Config: Name of the remote connector used for continuity")
    String getRemoteConnector();

    /* Operations */ 

    @Operation(desc = "Start the continuity service", impact = MBeanOperationInfo.ACTION)
    void startService() throws Exception;

    @Operation(desc = "Stop the continuity service", impact = MBeanOperationInfo.ACTION)
    void stopService() throws Exception;

    @Operation(desc = "Activate the site to start message delivery to target queues", impact = MBeanOperationInfo.ACTION)
    void activateSite() throws Exception;

    @Operation(desc = "Initialize the continuity command manager", impact = MBeanOperationInfo.ACTION)
    void initializeCommandManager() throws Exception;

    @Operation(desc = "Start the continuity command manager", impact = MBeanOperationInfo.ACTION)
    void startCommandManager() throws Exception;

    @Operation(desc = "Stop the continuity command manager", impact = MBeanOperationInfo.ACTION)
    void stopCommandManager() throws Exception;

    @Operation(desc = "Stop the continuity command manager", impact = MBeanOperationInfo.ACTION)
    void sendCommand(@Parameter(name = "action", desc = "Action to send: activate-site, broker-connect, add-address, add-queue, remove-address, remove-queue") String action, 
                     @Parameter(name = "address", desc = "Address name") String address, 
                     @Parameter(name = "queue", desc = "Queue name") String queue) throws Exception;

    @Operation(desc = "Stop the continuity command manager", impact = MBeanOperationInfo.ACTION)
    void handleCommand(@Parameter(name = "action", desc = "Action to send: activate-site, broker-connect, add-address, add-queue, remove-address, remove-queue") String action, 
                       @Parameter(name = "address", desc = "Address name") String address, 
                       @Parameter(name = "queue", desc = "Queue name") String queue) throws Exception;


}
