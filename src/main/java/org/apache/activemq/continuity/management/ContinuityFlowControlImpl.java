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

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;

import org.apache.activemq.artemis.core.management.impl.AbstractControl;
import org.apache.activemq.artemis.core.management.impl.MBeanInfoHelper;
import org.apache.activemq.continuity.core.AckInfo;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.apache.activemq.continuity.core.ContinuityService;

public class ContinuityFlowControlImpl extends AbstractControl implements ContinuityFlowControl {

    private final ContinuityService service; 
    private final ContinuityFlow flow;    
    
    public ContinuityFlowControlImpl(final ContinuityService service, final ContinuityFlow flow) throws Exception {
        super(ContinuityFlowControl.class, service.getServer().getStorageManager());
        this.service = service;
        this.flow = flow;
    }

    @Override
    protected MBeanOperationInfo[] fillMBeanOperationInfo() {
        return MBeanInfoHelper.getMBeanOperationsInfo(ContinuityFlowControl.class);
    }

    @Override
    protected MBeanAttributeInfo[] fillMBeanAttributeInfo() {
        return MBeanInfoHelper.getMBeanAttributesInfo(ContinuityFlowControl.class);
    }

    public ContinuityService getService() {
        return service;
    }

    public ContinuityFlow getFlow() {
        return flow;
    }

    /* Facts */

    public String getFlowInitialized() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getFlowInitialized(flow);
        }
        clearIO();
        try {
            return Boolean.toString(flow.isInitialized());
        } finally {
            blockOnIO();
        }
    }

    public String getFlowStarted() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getFlowStarted(flow);
        }
        clearIO();
        try {
            return Boolean.toString(flow.isStarted());
        } finally {
            blockOnIO();
        }
    }


    public String getSubjectAddressName() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getSubjectAddressName(flow);
        }
        clearIO();
        try {
            return flow.getSubjectAddressName();
        } finally {
            blockOnIO();
        }
    }

    public String getSubjectQueueName() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getSubjectQueueName(flow);
        }
        clearIO();
        try {
            return flow.getSubjectQueueName();
        } finally {
            blockOnIO();
        }
    }

    public String getOutflowMirrorName() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getOutflowMirrorName(flow);
        }
        clearIO();
        try {
            return flow.getOutflowMirrorName();
        } finally {
            blockOnIO();
        }
    }

    public String getOutflowMirrorBridgeName() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getOutflowMirrorBridgeName(flow);
        }
        clearIO();
        try {
            return flow.getOutflowMirrorBridgeName();
        } finally {
            blockOnIO();
        }
    }

    public String getOutflowAcksName() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getOutflowAcksName(flow);
        }
        clearIO();
        try {
            return flow.getOutflowAcksName();
        } finally {
            blockOnIO();
        }
    }

    public String getOutflowAcksBridgeName() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getOutflowAcksBridgeName(flow);
        }
        clearIO();
        try {
            return flow.getOutflowAcksBridgeName();
        } finally {
            blockOnIO();
        }
    }

    public String getInflowMirrorName() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getInflowMirrorName(flow);
        }
        clearIO();
        try {
            return flow.getInflowMirrorName();
        } finally {
            blockOnIO();
        }
    }

    public String getInflowAcksName() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getInflowAcksName(flow);
        }
        clearIO();
        try {
            return flow.getInflowAcksName();
        } finally {
            blockOnIO();
        }
    }

    public String getTargetBridgeName() {
        if (ContinuityAuditLogger.isEnabled() && flow != null) {
            ContinuityAuditLogger.getTargetBridgeName(flow);
        }
        clearIO();
        try {
            return flow.getTargetBridgeName();
        } finally {
            blockOnIO();
        }
    }

    /* Status */

    /* Statistics */

    public String getAverageAckDuration() {
        if (ContinuityAuditLogger.isEnabled() && flow != null && flow.getAckManager() != null) {
            ContinuityAuditLogger.getAverageAckDuration(flow);
        }
        clearIO();
        try {
            if(flow == null || flow.getAckManager() == null || flow.getAckManager().getAverageAckDuration() == null) {
                return "";
            } else {
                Double msAvg = flow.getAckManager().getAverageAckDuration();
                Double secAvg = msAvg / 1000;
                Double minAvg = secAvg / 60;    
                return String.format("%.6f ms (%.3f secs / %.2f mins)", msAvg, secAvg, minAvg);
            }
        } finally {
            blockOnIO();
        }
    }

    public String getMaxAckDuration() {
        if (ContinuityAuditLogger.isEnabled() && flow != null && flow.getAckManager() != null) {
            ContinuityAuditLogger.getMaxAckDuration(flow);
        }
        clearIO();
        try {
            if(flow == null || flow.getAckManager() == null || flow.getAckManager().getMaxAckDuration() == null) {
                return "";
            } else {
                Long msMax = flow.getAckManager().getMaxAckDuration();
                Double secMax = msMax.doubleValue() / 1000;
                Double minMax = secMax / 60;    
                return String.format("%d ms (%.3f secs / %.2f mins)", msMax, secMax, minMax);
            }
        } finally {
            blockOnIO();
        }
    }

    public String getMinAckDuration() {
        if (ContinuityAuditLogger.isEnabled() && flow != null && flow.getAckManager() != null) {
            ContinuityAuditLogger.getMinAckDuration(flow);
        }
        clearIO();
        try {
            if(flow == null || flow.getAckManager() == null || flow.getAckManager().getMinAckDuration() == null) {
                return "";
            } else {
                Long msMin = flow.getAckManager().getMinAckDuration();
                Double secMin = msMin.doubleValue() / 1000;
                Double minMin = secMin / 60;    
                return String.format("%d ms (%.3f secs / %.2f mins)", msMin, secMin, minMin);
            }
        } finally {
            blockOnIO();
        }
    }

    /* Volatile Configuration */

    public Boolean getAddDuplicatesToTarget() {
        if (ContinuityAuditLogger.isEnabled() && flow != null && flow.getAckManager() != null) {
            ContinuityAuditLogger.isAddDuplicatesToTarget(flow);
        }
        clearIO();
        try {
            if(flow == null || flow.getAckManager() == null)
                return null;
            else
                return flow.getAckManager().isAddDuplicatesToTarget();
        } finally {
            blockOnIO();
        }
    }
    
    public Boolean getRemoveMessageFromMirror() {
        if (ContinuityAuditLogger.isEnabled() && flow != null && flow.getAckManager() != null) {
            ContinuityAuditLogger.isRemoveMessageFromMirror(flow);
        }
        clearIO();
        try {
            if(flow == null || flow.getAckManager() == null)
                return null;
            else
                return flow.getAckManager().isRemoveMessageFromMirror();
        } finally {
            blockOnIO();
        }
    }
    
    public Boolean getDelayMessageOnInflow() {
        if (ContinuityAuditLogger.isEnabled() && flow != null && flow.getAckManager() != null) {
            ContinuityAuditLogger.isDelayMessageOnInflow(flow);
        }
        clearIO();
        try {
            if(flow == null || flow.getAckManager() == null)
                return null;
            else
                return flow.getAckManager().isDelayMessageOnInflow();
        } finally {
            blockOnIO();
        }
    }

     /* Static Configuration */

    /* Operations */ 

    public void handleAck(String queueName, Date sendTime, Date ackTime) throws Exception {
        if (ContinuityAuditLogger.isEnabled() && flow != null && flow.getAckManager() != null) {
            ContinuityAuditLogger.handleAck(flow, queueName, sendTime, ackTime);
        }
        clearIO();
        try {
            if(flow == null || flow.getAckManager() == null) {
                AckInfo ack = new AckInfo();
                ack.setSourceQueueName(queueName);
                ack.setMessageSendTime(sendTime);
                ack.setAckTime(ackTime);
                flow.getAckManager().handleAck(ack);
            }
        } finally {
           blockOnIO();
        }
    }

    public void setAddDuplicatesToTarget(Boolean isAddDuplicatesToTarget) {
        if (ContinuityAuditLogger.isEnabled() && flow != null && flow.getAckManager() != null) {
            ContinuityAuditLogger.setAddDuplicatesToTarget(flow, isAddDuplicatesToTarget);
        }
        clearIO();
        try {
            if(flow != null && flow.getAckManager() != null) {
                flow.getAckManager().setAddDuplicatesToTarget(isAddDuplicatesToTarget);
            }
        } finally {
           blockOnIO();
        }
    }

    public void setRemoveMessageFromMirror(Boolean isRemoveMessageFromMirror) {
        if (ContinuityAuditLogger.isEnabled() && flow != null && flow.getAckManager() != null) {
            ContinuityAuditLogger.setRemoveMessageFromMirror(flow, isRemoveMessageFromMirror);
        }
        clearIO();
        try {
            if(flow != null && flow.getAckManager() != null) {
                flow.getAckManager().setRemoveMessageFromMirror(isRemoveMessageFromMirror);
            }
        } finally {
           blockOnIO();
        }
    }

    public void setDelayMessageOnInflow(Boolean isDelayMessageOnInflow) {
        if (ContinuityAuditLogger.isEnabled() && flow != null && flow.getAckManager() != null) {
            ContinuityAuditLogger.setDelayMessageOnInflow(flow, isDelayMessageOnInflow);
        }
        clearIO();
        try {
            if(flow != null && flow.getAckManager() != null) {
                flow.getAckManager().setDelayMessageOnInflow(isDelayMessageOnInflow);
            }
        } finally {
           blockOnIO();
        }
    }


}
