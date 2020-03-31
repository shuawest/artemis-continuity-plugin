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

public interface ContinuityBootstrapControl {

    /* Status */
    
    @Attribute(desc = "Status: whether the site is activated or not")
    Boolean getBooted();

    /* Operations */ 

    @Operation(desc = "Boot the continuity service", impact = MBeanOperationInfo.ACTION)
    void boot() throws Exception;

    @Operation(desc = "Destroy and Boot the continuity service with new settings", impact = MBeanOperationInfo.ACTION)
    void reboot() throws Exception;

    @Operation(desc = "Destroy the continuity service", impact = MBeanOperationInfo.ACTION)
    void destroy() throws Exception;

    @Operation(desc = "Boot the continuity service", impact = MBeanOperationInfo.ACTION)
    void configure(@Parameter(name = "siteId", desc = "Site Id") String siteId, 
                   @Parameter(name = "activeOnStart", desc = "Active on Start") Boolean activeOnStart, 
                   @Parameter(name = "servingAcceptors", desc = "Serving Acceptors") String servingAcceptors,
                   @Parameter(name = "localConnectorRef", desc = "Local Connector") String localConnectorRef,
                   @Parameter(name = "remoteConnectorRefs", desc = "Remote Connectors (, or ; separated)") String remoteConnectorRefs,               
                   @Parameter(name = "reorgManagement", desc = "Reorg Management Hierarchy") Boolean reorgManagement) throws Exception;

    @Operation(desc = "Set continuity secrets", impact = MBeanOperationInfo.ACTION)
    void setSecrets(@Parameter(name = "localContinuityUser", desc = "Local Continuity User") String localContinuityUser,
                    @Parameter(name = "localContinuityPass", desc = "Local Continuity Pass") String localContinuityPass,
                    @Parameter(name = "remoteContinuityUser", desc = "Remote Continuity Pass") String remoteContinuityUser,
                    @Parameter(name = "remoteContinuityPass", desc = "Remote Continuity Pass") String remoteontinuityPass) throws Exception;

    @Operation(desc = "Override continuity service tuning defaults", impact = MBeanOperationInfo.ACTION)
    void tune(@Parameter(name = "inflowStagingDelay", desc = "Inflow Staging Delay (ms)") Long inflowStagingDelay,
              @Parameter(name = "bridgeInterval", desc = "Bridge Interval (ms)") Long bridgeInterval,
              @Parameter(name = "bridgeIntervalMultiplier", desc = "Bridge Interval Multiplier") Double bridgeIntervalMultiplier,
              @Parameter(name = "pollDuration", desc = "Poll Duration (ms)") Long pollDuration,
              @Parameter(name = "activationTimeout", desc = "Activation Timeout (ms)") Long activationTimeout) throws Exception;

}
