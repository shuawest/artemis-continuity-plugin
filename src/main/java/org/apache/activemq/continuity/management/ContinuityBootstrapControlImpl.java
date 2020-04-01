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

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;

import org.apache.activemq.artemis.core.management.impl.AbstractControl;
import org.apache.activemq.artemis.core.management.impl.MBeanInfoHelper;
import org.apache.activemq.continuity.core.ContinuityBootstrapService;

public class ContinuityBootstrapControlImpl extends AbstractControl implements ContinuityBootstrapControl {
    
    public static final String CONTINUITY_BOOTSTAP_NAME = "continuity.bootstrap";

    private final ContinuityBootstrapService bootstrapService; 

    public ContinuityBootstrapControlImpl(final ContinuityBootstrapService bootstrapService) throws Exception {
        super(ContinuityBootstrapControl.class, bootstrapService.getServer().getStorageManager());
        this.bootstrapService = bootstrapService;
    }

    @Override
    protected MBeanOperationInfo[] fillMBeanOperationInfo() {
        return MBeanInfoHelper.getMBeanOperationsInfo(ContinuityBootstrapControl.class);
    }

    @Override
    protected MBeanAttributeInfo[] fillMBeanAttributeInfo() {
        return MBeanInfoHelper.getMBeanAttributesInfo(ContinuityBootstrapControl.class);
    }

    @Override
    public Boolean getBooted() {
        if (ContinuityBootstrapAuditLogger.isEnabled()) {
            ContinuityBootstrapAuditLogger.isBooted(bootstrapService);
        }
        clearIO();
        try {
            return bootstrapService.isBooted();
        } finally {
            blockOnIO();
        }  
    }

    @Override
    public void boot() throws Exception {
        if (ContinuityBootstrapAuditLogger.isEnabled()) {
            ContinuityBootstrapAuditLogger.boot(bootstrapService);
        }
        clearIO();
        try {
            bootstrapService.boot();
        } finally {
            blockOnIO();
        } 
    }

    @Override
    public void reboot() throws Exception {
        if (ContinuityBootstrapAuditLogger.isEnabled()) {
            ContinuityBootstrapAuditLogger.reboot(bootstrapService);
        }
        clearIO();
        try {
            bootstrapService.reboot();
        } finally {
            blockOnIO();
        } 
    }

    @Override
    public void destroy() throws Exception {
        if (ContinuityBootstrapAuditLogger.isEnabled()) {
            ContinuityBootstrapAuditLogger.destroy(bootstrapService);
        }
        clearIO();
        try {
            bootstrapService.destroy();
        } finally {
            blockOnIO();
        } 
    }

    @Override
    public void configure(String siteId, Boolean activeOnStart, String servingAcceptors, String localConnectorRef, String remoteConnectorRefs, Boolean reorgManagement) throws Exception {
        if (ContinuityBootstrapAuditLogger.isEnabled()) {
            ContinuityBootstrapAuditLogger.configure(bootstrapService, siteId, activeOnStart, servingAcceptors, localConnectorRef, remoteConnectorRefs, reorgManagement);
        }
        clearIO();
        try {
            bootstrapService.configure(siteId, activeOnStart, servingAcceptors, localConnectorRef, remoteConnectorRefs, reorgManagement);
        } finally {
            blockOnIO();
        } 
    }

    @Override
    public void setSecrets(String localContinuityUser, String localContinuityPass, String remoteContinuityUser, String remoteontinuityPass) throws Exception {        
        if (ContinuityBootstrapAuditLogger.isEnabled()) {
            ContinuityBootstrapAuditLogger.setSecrets(bootstrapService, localContinuityUser, localContinuityPass, remoteContinuityUser, remoteontinuityPass);
        }
        clearIO();
        try {
            bootstrapService.setSecrets(localContinuityUser, localContinuityPass, remoteContinuityUser, remoteontinuityPass);
        } finally {
            blockOnIO();
        } 
    }

    @Override
    public void tune(Long activationTimeout, Long inflowStagingDelay, Long bridgeInterval, Double bridgeIntervalMultiplier, Long pollDuration) throws Exception {        
        if (ContinuityBootstrapAuditLogger.isEnabled()) {
            ContinuityBootstrapAuditLogger.tune(bootstrapService, activationTimeout, inflowStagingDelay, bridgeInterval, bridgeIntervalMultiplier, pollDuration);
        }
        clearIO();
        try {
            bootstrapService.tune(activationTimeout, inflowStagingDelay, bridgeInterval, bridgeIntervalMultiplier, pollDuration);
        } finally {
            blockOnIO();
        } 
    }

}
