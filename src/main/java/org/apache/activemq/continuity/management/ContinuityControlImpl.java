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
import org.apache.activemq.continuity.core.ContinuityCommand;
import org.apache.activemq.continuity.core.ContinuityService;

public class ContinuityControlImpl extends AbstractControl implements ContinuityControl {

    private final ContinuityService service; 
    
    public ContinuityControlImpl(final ContinuityService service) throws Exception {
        super(ContinuityControl.class, service.getServer().getStorageManager());
        this.service = service;
    }

    @Override
    protected MBeanOperationInfo[] fillMBeanOperationInfo() {
        return MBeanInfoHelper.getMBeanOperationsInfo(ContinuityControl.class);
    }

    @Override
    protected MBeanAttributeInfo[] fillMBeanAttributeInfo() {
        return MBeanInfoHelper.getMBeanAttributesInfo(ContinuityControl.class);
    }

    /* Status */

    @Override
    public String getServiceInitialized() {
        if (ContinuityAuditLogger.isEnabled() && service != null) {
            ContinuityAuditLogger.isServiceInitialized(service);
        }
        clearIO();
        try {
            if(service == null)
                return "Continuity Service does not exist";
            else
                return Boolean.toString(service.isInitialized()); 
        } finally {
            blockOnIO();
        }
    }

    @Override
    public String getServiceStarted() {
        if (ContinuityAuditLogger.isEnabled() && service != null) {
            ContinuityAuditLogger.isServiceStarted(service);
        }
        clearIO();
        try {
            if(service == null)
                return "Continuity Service does not exist";
            else
                return Boolean.toString(service.isStarted()); 
        } finally {
            blockOnIO();
        }
    }

    @Override 
    public String getCommandManagerInitialized() {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getCommandManager() != null) {
            ContinuityAuditLogger.isCommandManagerInitialized(service.getCommandManager());
        }
        clearIO();
        try {
            if(service == null || service.getCommandManager() == null)
                return "Continuity Command Manager does not exist";
            else
                return Boolean.toString(service.getCommandManager().isInitialized());
        } finally {
            blockOnIO();
        }
    }

    @Override
    public String getCommandManagerStarted() {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getCommandManager() != null) {
            ContinuityAuditLogger.isCommandManagerStarted(service.getCommandManager());
        }
        clearIO();
        try {
            if(service == null || service.getCommandManager() == null)
                return "Continuity Command Manager does not exist";
            else
                return Boolean.toString(service.getCommandManager().isStarted());
        } finally {
            blockOnIO();
        }
    }

    /* Statistics */

    /* Volatile Configuration */

    /* Static Configuration */

    @Override
    public String getSiteId() {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getSiteId(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return service.getConfig().getSiteId();
        } finally {
            blockOnIO();
        }
    }

    @Override
    public String getLocalInVmUri() {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getLocalInVmUri(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return service.getConfig().getLocalInVmUri();
        } finally {
            blockOnIO();
        }
    }
 
    @Override
    public String getLocalUsername() {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getLocalUsername(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return service.getConfig().getLocalUsername();
        } finally {
            blockOnIO();
        }
    }
 
    @Override
    public String getBridgeInterval() {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getBridgeInterval(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return Long.toString(service.getConfig().getBridgeInterval());
        } finally {
            blockOnIO();
        }
    }
    
    @Override
    public String getBridgeIntervalMultiplier() {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getBridgeIntervalMultiplier(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return Double.toString(service.getConfig().getBridgeIntervalMultiplier());
        } finally {
            blockOnIO();
        }
    }

    @Override
    public String getOutflowMirrorSuffix() {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getOutflowMirrorSuffix(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return service.getConfig().getOutflowMirrorSuffix();
        } finally {
            blockOnIO();
        }
    }

    @Override
    public String getOutflowAcksSuffix()  {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getOutflowAcksSuffix(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return service.getConfig().getOutflowAcksSuffix();
        } finally {
            blockOnIO();
        }
    }

    @Override
    public String getInflowMirrorSuffix()  {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getInflowMirrorSuffix(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return service.getConfig().getInflowMirrorSuffix();
        } finally {
            blockOnIO();
        }
    }
    
    @Override
    public String getInflowAcksSuffix()  {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getInflowAcksSuffix(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return service.getConfig().getInflowAcksSuffix();
        } finally {
            blockOnIO();
        }
    }
    
    @Override
    public String getInflowStagingDelay()  {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getInflowStagingDelay(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return Long.toString(service.getConfig().getInflowStagingDelay());
        } finally {
            blockOnIO();
        }
    }
    
    @Override
    public String getCommandDestinationPrefix() {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getCommandDestinationPrefix(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return service.getConfig().getCommandDestinationPrefix();
        } finally {
            blockOnIO();
        }
    }
    
    @Override
    public String getLocalConnector()  {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getLocalConnector(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return service.getConfig().getLocalConnectorRef();
        } finally {
            blockOnIO();
        }
    }
    
    @Override
    public String getRemoteConnector()  {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getConfig() != null) {
            ContinuityAuditLogger.getRemoteConnector(service.getConfig());
        }
        clearIO();
        try {
            if(service == null || service.getConfig() == null)
                return "Continuity configuration does not exist";
            else
                return service.getConfig().getRemoteConnectorRef();
        } finally {
            blockOnIO();
        }
    }

    /* Operations */ 

    @Override
    public void startService() throws Exception {
        if (ContinuityAuditLogger.isEnabled() && service != null) {
           ContinuityAuditLogger.startService(service);
        }
        clearIO();
        try {
            if(service != null)
               service.start();
        } finally {
           blockOnIO();
        }
    }

    @Override
    public void stopService() throws Exception {
        if (ContinuityAuditLogger.isEnabled() && service != null) {
           ContinuityAuditLogger.stopService(service);
        }
        clearIO();
        try {
            if(service != null)
               service.stop();
        } finally {
           blockOnIO();
        }
    }

    @Override
    public void activateSite() throws Exception {
        if (ContinuityAuditLogger.isEnabled() && service != null) {
           ContinuityAuditLogger.activateSite(service);
        }
        clearIO();
        try {
            if(service != null)
               service.activateSite();
        } finally {
           blockOnIO();
        }
    }

    @Override
    public void initializeCommandManager() throws Exception {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getCommandManager() != null) {
           ContinuityAuditLogger.initializeCommandManager(service.getCommandManager());
        }
        clearIO();
        try {
            if(service != null && service.getCommandManager() != null)
               service.getCommandManager().initialize();
        } finally {
           blockOnIO();
        }
    }

    @Override
    public void startCommandManager() throws Exception {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getCommandManager() != null) {
           ContinuityAuditLogger.startCommandManager(service.getCommandManager());
        }
        clearIO();
        try {
            if(service != null && service.getCommandManager() != null)
               service.getCommandManager().start();
        } finally {
           blockOnIO();
        }
    }

    @Override
    public void stopCommandManager() throws Exception {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getCommandManager() != null) {
           ContinuityAuditLogger.stopCommandManager(service.getCommandManager());
        }
        clearIO();
        try {
            if(service != null && service.getCommandManager() != null)
               service.getCommandManager().stop();
        } finally {
           blockOnIO();
        }
    }

    @Override
    public void sendCommand(String action, String address, String queue) throws Exception {
        if (ContinuityAuditLogger.isEnabled() && service != null && service.getCommandManager() != null) {
           ContinuityAuditLogger.sendCommand(service.getCommandManager());
        }
        clearIO();
        try {
            if(service != null && service.getCommandManager() != null) {
                ContinuityCommand cmd = new ContinuityCommand();
                cmd.setAction(action);
                cmd.setAddress(address);
                cmd.setQueue(queue);
                service.getCommandManager().sendCommand(cmd);
            }
        } finally {
           blockOnIO();
        }
    }

    @Override
    public void handleCommand(String action, String address, String queue) throws Exception {
        if (ContinuityAuditLogger.isEnabled() && service != null) {
           ContinuityAuditLogger.handleCommand(service);
        }
        clearIO();
        try {
            if(service != null) {
                ContinuityCommand cmd = new ContinuityCommand();
                cmd.setAction(action);
                cmd.setAddress(address);
                cmd.setQueue(queue);
                service.handleIncomingCommand(cmd);
            }
        } finally {
           blockOnIO();
        }
    }

}
