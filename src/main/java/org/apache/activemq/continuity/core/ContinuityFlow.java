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

import java.util.Arrays;
import java.util.HashMap;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.core.config.DivertConfiguration;
import org.apache.activemq.artemis.core.config.TransformerConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ComponentConfigurationRoutingType;
import org.apache.activemq.artemis.core.server.QueueQueryResult;
import org.apache.activemq.artemis.core.server.cluster.Bridge;
import org.apache.activemq.continuity.plugins.OriginTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: look into creating a custom bridge to target queue, instead of full address
// TODO: refactor to have bridges on remote side, but consider 3+ site model
public class ContinuityFlow {

  private static final Logger log = LoggerFactory.getLogger(ContinuityFlow.class);

  private final ContinuityService service;
  private final QueueInfo queueInfo;

  private final String subjectAddressName;
  private final String subjectQueueName;
  private final String subjectQueueRoutingType;

  private final String outflowMirrorName;
  private final String outflowDivertName;
  private final String outflowMirrorBridgeName;

  private final String outflowAcksName;
  private final String outflowAcksBridgeName;

  private final String inflowMirrorName;
  private final String inflowAcksName;
  private final String targetBridgeName;

  private boolean isInitialized = false;
  private boolean isStarted = false;

  private Bridge outflowMirrorBridge;
  private Bridge outflowAcksBridge;
  private Bridge targetBridge;

  private AckInterceptor ackInterceptor;
  private AckReceiver ackReceiver;
  private AckManager ackManager;

  // TODO: fix to start flow on dynamic creation
  public ContinuityFlow(final ContinuityService service, final QueueInfo queueInfo) {
    this.service = service;
    this.queueInfo = queueInfo;

    this.subjectAddressName = queueInfo.getAddressName();
    this.subjectQueueName = queueInfo.getQueueName();
    this.subjectQueueRoutingType = queueInfo.getRoutingType();

    this.outflowMirrorName = subjectQueueName + getConfig().getOutflowMirrorSuffix();
    this.outflowDivertName = outflowMirrorName + ".divert";
    this.outflowMirrorBridgeName = outflowMirrorName + ".bridge";

    this.outflowAcksName = subjectQueueName + getConfig().getOutflowAcksSuffix();
    this.outflowAcksBridgeName = outflowAcksName + ".bridge";

    this.inflowMirrorName = subjectQueueName + getConfig().getInflowMirrorSuffix();
    this.inflowAcksName = subjectQueueName + getConfig().getInflowAcksSuffix();
    this.targetBridgeName = inflowMirrorName + ".bridge";
  }

  public void initialize() throws ContinuityException {
    service.registerContinuityFlow(queueInfo.getQueueName(), this);
    
    createFlowQueue(outflowMirrorName, outflowMirrorName, RoutingType.MULTICAST);
    createDivert(outflowDivertName, subjectAddressName, outflowMirrorName);

    createFlowQueue(outflowAcksName, outflowAcksName, RoutingType.MULTICAST);
    createAckInterceptor();

    createFlowQueue(inflowMirrorName, inflowMirrorName, RoutingType.ANYCAST);
    createFlowQueue(inflowAcksName, inflowAcksName, RoutingType.ANYCAST);
    createAckManager();
    createAckReceiver();

    createMatchingQueue(queueInfo);

    isInitialized = true;

  }

  public void start() throws ContinuityException {
    ackInterceptor.start();
    ackReceiver.start();

    this.outflowMirrorBridge = createBridge(outflowMirrorBridgeName, outflowMirrorName, inflowMirrorName, RoutingType.ANYCAST, getConfig().getRemoteConnectorRef(), true);
    this.outflowAcksBridge = createBridge(outflowAcksBridgeName, outflowAcksName, inflowAcksName, RoutingType.ANYCAST, getConfig().getRemoteConnectorRef(), true);
    
    boolean isActivated = service.isActivated();
    this.targetBridge = createBridge(targetBridgeName, inflowMirrorName, subjectAddressName, RoutingType.valueOf(subjectQueueRoutingType), getConfig().getLocalConnectorRef(), isActivated);

    service.getManagement().registerContinuityFlow(service, this);

    isStarted = true;
  }

  public void startSubjectQueueDelivery() throws ContinuityException {
    try {
      if(isInitialized && isStarted) {
        startBridge(targetBridge);      
      }

    } catch (Exception e) {
      throw new ContinuityException("Unable to start subject queue bridge", e);
    }
  }

  public void stopSubjectQueueDelivery() throws ContinuityException {
    try {
      if(isInitialized && isStarted) {
        stopBridge(targetBridge);      
      }

    } catch (Exception e) {
      throw new ContinuityException("Unable to stop subject queue bridge", e);
    }
  }

  private void startBridge(Bridge bridge) throws ContinuityException {
    try {
      if(log.isDebugEnabled()) {
        log.debug("Starting bridge from '{}' to '{}''", bridge.getQueue().getName().toString(), bridge.getForwardingAddress());
      }

      if(bridge == null) {
        log.warn("Unable to start bridge for flow '{}', it doesn't exist", this.getSubjectQueueName());
      } else {
        bridge.start();
      }

    } catch (Exception e) {
      throw new ContinuityException("Unabled to start bridge", e);
    }
  }

  private void stopBridge(Bridge bridge) throws ContinuityException {
    try {
      if(log.isDebugEnabled()) {
        log.debug("Stopping bridge from '{}' to '{}''", bridge.getQueue().getName().toString(), bridge.getForwardingAddress());
      }

      if(bridge == null) {
        log.warn("Unable to stop bridge for flow '{}', it doesn't exist", this.getSubjectQueueName());
      } else {
        bridge.stop();
      }
      
    } catch (Exception e) {
      throw new ContinuityException("Unabled to start bridge", e);
    }
  }

  public void stop() throws ContinuityException {
    try {
      stopBridge(outflowMirrorBridge);
      stopBridge(outflowAcksBridge);
      stopBridge(targetBridge);
      ackInterceptor.stop();
      ackReceiver.stop();
      service.getManagement().unregisterFlowManagement(service, this);
    } catch (Exception e) {
      throw new ContinuityException("Unable to stop bridge", e);
    }
    
    isStarted = false;
  }


  private void createAckInterceptor() throws ContinuityException {
    this.ackInterceptor = new AckInterceptor(service, this);
  }
  
  private void createAckReceiver() throws ContinuityException {
    this.ackReceiver = new AckReceiver(service, this);
  }

  private void createAckManager() throws ContinuityException {
    this.ackManager = new AckManager(service, this);
  }

  private void createFlowQueue(final String addressName, final String queueName, final RoutingType routingType) throws ContinuityException {
    try {
      if(!queueExists(queueName)) {
        getServer().createQueue(SimpleString.toSimpleString(addressName), 
          routingType,
          SimpleString.toSimpleString(queueName), 
          SimpleString.toSimpleString(getConfig().getLocalUsername()), 
          null, true, false);
      }

    } catch (final Exception e) {
      log.error("Failed to create mirror queue: " + queueName, e);
      throw new ContinuityException("Failed to create mirror queue", e);
    }
  }

  private void createMatchingQueue(final QueueInfo queueInfo) throws ContinuityException {
    final String addressName = queueInfo.getAddressName();
    final String queueName = queueInfo.getQueueName();
    final String routingType = queueInfo.getRoutingType();

    // TOOO: update to match queue settings
    try {
      if(!queueExists(queueName)) {
        getServer().createQueue(SimpleString.toSimpleString(addressName), 
          RoutingType.valueOf(routingType),
          SimpleString.toSimpleString(queueName), 
          SimpleString.toSimpleString(getConfig().getLocalUsername()), 
          null, true, false);
      }

    } catch (final Exception e) {
      log.error("Failed to create mirror queue: " + queueName, e);
      throw new ContinuityException("Failed to create mirror queue", e);
    }
  }

  private boolean queueExists(final String queueName) throws ContinuityException {
    try {
      final QueueQueryResult queueSearch = getServer().queueQuery(SimpleString.toSimpleString(queueName));
      
      if(log.isTraceEnabled()) {
        log.trace("Checking if queue {} exists: {}", queueName, queueSearch.isExists());
      }
      
      return (queueSearch.isExists());

    } catch (final Exception e) {
      final String eMessage = String.format("Failed check if queue exists: %s", queueName);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  private void createDivert(final String divertName, final String sourceAddress, final String targetAddress) throws ContinuityException {
    try {
      final TransformerConfiguration originTransformer = new TransformerConfiguration(OriginTransformer.class.getName());
      originTransformer.setProperties(new HashMap<String, String>() {{
        put(OriginTransformer.ORIGIN_CONFIG, getConfig().getSiteId());
      }});

      String originFilterExp = String.format("%s <> '%s' OR %s IS NULL", OriginTransformer.ORIGIN_HEADER_NAME, getConfig().getSiteId(), OriginTransformer.ORIGIN_HEADER_NAME);

      final DivertConfiguration divert = new DivertConfiguration()
        .setName(divertName)
        .setAddress(sourceAddress)
        .setForwardingAddress(targetAddress)
        .setFilterString(originFilterExp)
        .setExclusive(false)
        .setTransformerConfiguration(originTransformer);

      getServer().deployDivert(divert);

    } catch (final Exception e) {
      final String eMessage = String.format("Failed to create divert: %s", sourceAddress);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  private Bridge createBridge(final String bridgeName, final String fromQueue, final String toAddress, final RoutingType targetRoutingType, final String connectorRef, final boolean start) throws ContinuityException {
    Bridge bridge = null;
    try {
      ComponentConfigurationRoutingType bridgeRoutingType;
      if(targetRoutingType == RoutingType.ANYCAST)
        bridgeRoutingType = ComponentConfigurationRoutingType.ANYCAST;
      else
        bridgeRoutingType = ComponentConfigurationRoutingType.MULTICAST; 

      // TODO: determine why messages sent over amqp arent bridged
      //  - not related to dup detection
      //  - not related to address string format
      //  - do note that messages sent to multicast without consumer connected are dropped, but are diverted to mirror
      final BridgeConfiguration bridgeConfig = new BridgeConfiguration()
          .setName(bridgeName)
          .setQueueName(fromQueue)
          .setForwardingAddress(toAddress)
          .setHA(true)
          .setRetryInterval(getConfig().getBridgeInterval())
          .setRetryIntervalMultiplier(getConfig().getBridgeIntervalMultiplier())
          .setInitialConnectAttempts(-1)
          .setReconnectAttempts(-1)
          //.setUseDuplicateDetection(true)
          .setRoutingType(bridgeRoutingType)
          .setConfirmationWindowSize(10000000)
          .setStaticConnectors(Arrays.asList(connectorRef)); 

      getServer().deployBridge(bridgeConfig);

      bridge = getServer().getClusterManager().getBridges().get(bridgeName);

      if(!start) {
        bridge.stop();
      }

    } catch (final Exception e) {
      final String eMessage = String.format("Failed to create divert from '%s' to '%s'", fromQueue, toAddress);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
    return bridge;
  }

  private ContinuityConfig getConfig() {
    return service.getConfig();
  }
  private ActiveMQServer getServer() {
    return service.getServer();
  }

  public boolean isInitialized() {
    return isInitialized;
  }
  public boolean isStarted() {
    return isStarted;
  }

  public AckInterceptor getAckInterceptor() {
    return ackInterceptor;
  }
  public AckReceiver getAckReceiver() {
    return ackReceiver;
  }
  public AckManager getAckManager() {
    return ackManager;
  }

  /** flow element names **/

  public String getSubjectAddressName() {
    return subjectAddressName;
  }
  public String getSubjectQueueName() {
    return subjectQueueName;
  }
  public String getSubjectQueueRoutingType() {
    return subjectQueueRoutingType;
  }

  public String getOutflowDivertName() {
    return outflowDivertName;
  }
  public String getOutflowMirrorName() {
    return outflowMirrorName;
  }
  public String getOutflowMirrorBridgeName() {
    return outflowMirrorBridgeName;
  }
  public String getOutflowAcksName() {
    return outflowAcksName;
  }
  public String getOutflowAcksBridgeName() {
    return outflowAcksBridgeName;
  }

  public String getInflowMirrorName() {
    return inflowMirrorName;
  }
  public String getInflowAcksName() {
    return inflowAcksName;
  }
  public String getTargetBridgeName() {
    return targetBridgeName;
  }

}
