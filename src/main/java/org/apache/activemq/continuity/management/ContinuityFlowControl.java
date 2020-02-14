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

import java.util.Date;

import javax.management.MBeanOperationInfo;

import org.apache.activemq.artemis.api.core.management.Attribute;
import org.apache.activemq.artemis.api.core.management.Operation;
import org.apache.activemq.artemis.api.core.management.Parameter;

public interface ContinuityFlowControl {

    /* Facts */

    @Attribute(desc = "Config: Name of the subject/target address for the continuity flow")
    String getSubjectAddressName();

    @Attribute(desc = "Config: Name of the subject/target queue for the continuity flow")
    String getSubjectQueueName();

    @Attribute(desc = "Config: Name of the outflow mirror address/queue for the continuity flow")
    String getOutflowMirrorName();

    @Attribute(desc = "Config: Name of the outflow mirror bridge for the continuity flow")
    String getOutflowMirrorBridgeName();

    @Attribute(desc = "Config: Name of the outflow acks address/queue for the continuity flow")
    String getOutflowAcksName();

    @Attribute(desc = "Config: Name of the outflow acks bridge for the continuity flow")
    String getOutflowAcksBridgeName();

    @Attribute(desc = "Config: Name of the inflow mirror address/queue for the continuity flow")
    String getInflowMirrorName();

    @Attribute(desc = "Config: Name of the inflow acks address/queue for the continuity flow")
    String getInflowAcksName();

    @Attribute(desc = "Config: Name of the target bridge for the continuity flow")
    String getTargetBridgeName();

    /* Status */

    /* Statistics */

    /* Volatile Configuration */

    @Attribute(desc = "Config: Add duplicate ids to the target queue")
    String isAddDuplicatesToTarget();
    
    @Attribute(desc = "Config: Remove messages from staging queue")
    String isRemoveMessageFromMirror();
    
    @Attribute(desc = "Config: Delay message on staging queue")
    String isDelayMessageOnInflow();

     /* Static Configuration */

    /* Operations */ 

    @Operation(desc = "Handle an ack to mark a message for removal", impact = MBeanOperationInfo.ACTION)
    void handleAck(@Parameter(name = "queueName", desc = "Name of the target queue") String queueName, 
                   @Parameter(name = "sendTime", desc = "Time the original message was sent (affects statistic tracking)") Date sendTime, 
                   @Parameter(name = "ackTime", desc = "Time of the acknowledgement  (affects statistic tracking)") Date ackTime) throws Exception;

    @Operation(desc = "Add duplicate ids to the target queue to prevent delivery", impact = MBeanOperationInfo.ACTION)
    void setAddDuplicatesToTarget(@Parameter(name = "isAddDuplicatesToTarget", desc = "true or false") boolean isAddDuplicatesToTarget);

    @Operation(desc = "Remove messages from staging queue", impact = MBeanOperationInfo.ACTION)
    void setRemoveMessageFromMirror(@Parameter(name = "isRemoveMessageFromMirror", desc = "true or false") boolean isRemoveMessageFromMirror);

    @Operation(desc = "Delay messages on staging queue", impact = MBeanOperationInfo.ACTION)
    void setDelayMessageOnInflow(@Parameter(name = "isDelayMessageOnInflow", desc = "true or false") boolean isDelayMessageOnInflow);

}
