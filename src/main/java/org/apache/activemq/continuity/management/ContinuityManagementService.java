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

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.activemq.artemis.api.core.management.ObjectNameBuilder;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.management.ManagementService;
import org.apache.activemq.continuity.core.ContinuityException;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.apache.activemq.continuity.core.ContinuityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityManagementService {

    private static final Logger log = LoggerFactory.getLogger(ContinuityManagementService.class);

    public static final String CONTINUITY_SERVICE_NAME = "continuity.service";

    private final ManagementService managementService;

    public ContinuityManagementService(final ManagementService managementService) {
        this.managementService = managementService;
    }

    public ManagementService getManagementService() {
        return managementService;
    }

    public ObjectNameBuilder getObjectNameBuilder() {
        return managementService.getObjectNameBuilder();
    }

    public ObjectName getBrokerObjectName() throws Exception {
        return getObjectNameBuilder().getActiveMQServerObjectName();
    }

    public String getBrokerPrefix() throws Exception {
        return getBrokerObjectName().toString().trim();
    }

    public ObjectName getContinuityServiceName() throws Exception {
        String name = String.format("%s,component=continuity,name=%s", getBrokerPrefix(), ObjectName.quote(CONTINUITY_SERVICE_NAME));
        return ObjectName.getInstance(name);
    }

    public String getContinuityServicePrefix() throws Exception {
        return getContinuityServiceName().toString().trim();
    }

    public ObjectName getContinuityFlowName(ContinuityService service, ContinuityFlow flow) throws Exception {
        String continuityPrefix = getContinuityServiceName().toString().trim();
        String flowName = String.format("%s,subcomponent=flows,flow=%s", continuityPrefix,
                ObjectName.quote(flow.getSubjectQueueName()));
        return ObjectName.getInstance(flowName);
    }

    public String getContinuityFlowPrefix(ContinuityService service, ContinuityFlow flow) throws Exception {
        return getContinuityFlowName(service, flow).toString().trim();
    }

    public void registerContinuityService(ContinuityService service) throws ContinuityException {
        try {
            ObjectName name = getContinuityServiceName();
            if (log.isDebugEnabled()) {
                log.debug("Registering continuity service for management: {}", name);
            }
            ContinuityControl continuityControl = new ContinuityControlImpl(service);
            managementService.registerInJMX(name, continuityControl);
            managementService.registerInRegistry(CONTINUITY_SERVICE_NAME, continuityControl);

            if(service.getConfig().isReorgManagmentHierarchy()) {
                moveServicePrimitiveMBeans(service);
            }
        } catch (Exception e) {
            String msg = "Unable to register continuity service for management";
            log.error(msg, e);
            throw new ContinuityException(msg, e);
        }
    }

    public synchronized void unregisterContinuityService() throws ContinuityException {
        try {
            ObjectName name = getContinuityServiceName();
            if (log.isDebugEnabled()) {
                log.debug("Registering continuity service for management: {}", name);
            }
            managementService.unregisterFromJMX(name);
            managementService.unregisterFromRegistry(CONTINUITY_SERVICE_NAME);
        } catch (Exception e) {
            String msg = "Unable to register continuity service for management";
            log.error(msg, e);
            throw new ContinuityException(msg, e);
        }
    }

    public void registerContinuityFlow(ContinuityService service, ContinuityFlow flow) throws ContinuityException {
        try {
            ObjectName continuityFlowName = getContinuityFlowName(service, flow);
            ContinuityFlowControl continuityFlowControl = new ContinuityFlowControlImpl(service, flow);
            managementService.registerInJMX(continuityFlowName, continuityFlowControl);
            managementService.registerInRegistry("flow." + flow.getSubjectQueueName(), continuityFlowControl);

            if(service.getConfig().isReorgManagmentHierarchy()) {
                moveFlowPrimitiveMBeans(service, flow);
            }
        } catch (Exception e) {
            String msg = "Unable to register continuity flow for management";
            log.error(msg, e);
            throw new ContinuityException(msg, e);
        }
    }

    public synchronized void unregisterFlowManagement(ContinuityService service, ContinuityFlow flow)
            throws ContinuityException {
        try {
            ObjectName continuityFlowName = getContinuityFlowName(service, flow);
            managementService.unregisterFromJMX(continuityFlowName);
            managementService.unregisterFromRegistry("flow." + flow.getSubjectQueueName());
        } catch (Exception e) {
            String msg = "Unable to register continuity service for management";
            log.error(msg, e);
            throw new ContinuityException(msg, e);
        }
    }

    // Consider doing these moves in place within the service
    public void moveServicePrimitiveMBeans(ContinuityService service) throws ContinuityException {
        try {
            String target = getContinuityServicePrefix();
            moveAddressControl(target, "", service.getCommandManager().getCommandInQueueName());
            moveAddressControl(target, "", service.getCommandManager().getCommandOutQueueName());
            moveQueueControl(target, "", service.getCommandManager().getCommandInQueueName(), "anycast");
            moveQueueControl(target, "", service.getCommandManager().getCommandOutQueueName(), "multicast");
            moveBridgeControl(target, "", service.getCommandManager().getCommandOutBridgeName());
        } catch (Exception e) {
            String msg = "Unable to change name of continuity service primitives in management hierarchy";
            log.error(msg, e);
            throw new ContinuityException(msg, e);
        }
    }

    // Consider doing these moves in place within the flow, instead they are created at different times
    public void moveFlowPrimitiveMBeans(ContinuityService service, ContinuityFlow flow) throws ContinuityException {
        try {
            String target = getContinuityFlowPrefix(service, flow);
            moveAddressControl(target, "sub", flow.getInflowAcksName());
            moveAddressControl(target, "sub", flow.getInflowMirrorName());
            moveAddressControl(target, "sub", flow.getOutflowAcksName());
            moveAddressControl(target, "sub", flow.getOutflowMirrorName());
            moveQueueControl(target, "sub", flow.getInflowAcksName(), "anycast");
            moveQueueControl(target, "sub", flow.getInflowMirrorName(), "anycast");
            moveQueueControl(target, "sub", flow.getOutflowAcksName(), "multicast");
            moveQueueControl(target, "sub", flow.getOutflowMirrorName(), "multicast");
            moveDivertControl(target, "sub", flow.getSubjectAddressName(), flow.getOutflowDivertName());
            moveBridgeControl(target, "sub", flow.getOutflowAcksBridgeName());
            moveBridgeControl(target, "sub", flow.getOutflowMirrorBridgeName());
            moveBridgeControl(target, "sub", flow.getTargetBridgeName());
        } catch (Exception e) {
            String msg = "Unable to change name of continuity flow primitives in management hierarchy";
            log.error(msg, e);
            throw new ContinuityException(msg, e);
        }
    }

    public void moveAddressControl(String target, String subLevel, String addressName) throws Exception {
        String originalName = String.format("%s,component=addresses,address=\"%s\"", getBrokerPrefix(), addressName);
        String newName = String.format("%s,%ssubcomponent=addresses,address=\"%s\"", target, subLevel, addressName);
        String resourceName = ResourceNames.ADDRESS + addressName;
        renameObject(resourceName, originalName, newName);
    }

    public void moveBridgeControl(String target, String subLevel, String bridgeName) throws Exception {
        String originalName = String.format("%s,component=bridges,name=\"%s\"", getBrokerPrefix(), bridgeName);
        String newName = String.format("%s,%ssubcomponent=bridges,bridge=\"%s\"", target, subLevel, bridgeName);
        String resourceName = ResourceNames.BRIDGE + bridgeName;
        renameObject(resourceName, originalName, newName);
    }

    public void moveDivertControl(String target, String subLevel, String addressName, String divertName) throws Exception {
        String originalName = String.format("%s,component=addresses,address=\"%s\",subcomponent=diverts,divert=\"%s\"", getBrokerPrefix(), addressName, divertName);
        String newName = String.format("%s,%ssubcomponent=diverts,divert=\"%s\"", target, subLevel, divertName);
        String resourceName = ResourceNames.DIVERT + divertName;
        renameObject(resourceName, originalName, newName);
    }

    public void moveQueueControl(String target, String subLevel, String addressQueueName, String routingType) throws Exception {
        String originalName = String.format("%s,component=addresses,address=\"%s\",subcomponent=queues,routing-type=\"%s\",queue=\"%s\"", getBrokerPrefix(), addressQueueName, routingType, addressQueueName);
        String newName = String.format("%s,%ssubcomponent=addresses,address=\"%s\",%ssubsubcomponent=queues,routing-type=\"%s\",queue=\"%s\"", target, subLevel, addressQueueName, subLevel, routingType, addressQueueName);
        String resourceName = ResourceNames.QUEUE + addressQueueName;
        renameObject(resourceName, originalName, newName);
    }

    private void renameObject(String registryName, String originalName, String newName) throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        Object control = managementService.getResource(registryName);
        mBeanServer.unregisterMBean(new ObjectName(originalName));
        mBeanServer.registerMBean(control, new ObjectName(newName));       

    }
}