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

  private AckDivert ackDivert;
  private AckReceiver ackReceiver;
  private AckManager ackManager;

  public ContinuityFlow(final ContinuityService service, final QueueInfo queueInfo) {
    this.service = service;
    this.queueInfo = queueInfo;

    this.subjectAddressName = queueInfo.getAddressName();
    this.subjectQueueName = queueInfo.getQueueName();

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
    
    createFlowQueue(outflowMirrorName, outflowMirrorName);
    createDivert(outflowDivertName, subjectAddressName, outflowMirrorName);

    createFlowQueue(outflowAcksName, outflowAcksName);
    createAckDivert();

    createFlowQueue(inflowMirrorName, inflowMirrorName);
    createFlowQueue(inflowAcksName, inflowAcksName);
    createAckManager();
    createAckReceiver();

    createMatchingQueue(queueInfo);

    isInitialized = true;

  }

  public void start() throws ContinuityException {
    ackDivert.start();
    ackReceiver.start();

    this.outflowMirrorBridge = createBridge(outflowMirrorBridgeName, outflowMirrorName, inflowMirrorName, getConfig().getRemoteConnectorRef(), true);
    this.outflowAcksBridge = createBridge(outflowAcksBridgeName, outflowAcksName, inflowAcksName, getConfig().getRemoteConnectorRef(), true);
    this.targetBridge = createBridge(targetBridgeName, inflowMirrorName, subjectAddressName, getConfig().getLocalConnectorRef(), false);
    //startBridge(outflowMirrorBridge);
    //startBridge(outflowAcksBridge);
    //stopBridge(targetBridge);

    service.getManagement().registerContinuityFlow(service, this);

    isStarted = true;
  }

  public void startSubjectQueueDelivery() throws ContinuityException {
    try {
      startBridge(targetBridge);      
    } catch (Exception e) {
      throw new ContinuityException("Unable to start subject queue bridge", e);
    }
  }

  private void startBridge(Bridge bridge) throws ContinuityException {
    try {
      if(log.isDebugEnabled()) {
        log.debug("Starting bridge from '{}' to '{}''", bridge.getQueue().getName().toString(), bridge.getForwardingAddress());
      }

      bridge.start();
    } catch (Exception e) {
      throw new ContinuityException("Unabled to start bridge", e);
    }
  }

  private void stopBridge(Bridge bridge) throws ContinuityException {
    try {
      if(log.isDebugEnabled()) {
        log.debug("Stopping bridge from '{}' to '{}''", bridge.getQueue().getName().toString(), bridge.getForwardingAddress());
      }

      bridge.stop();
    } catch (Exception e) {
      throw new ContinuityException("Unabled to start bridge", e);
    }
  }

  public void stop() throws ContinuityException {
    try {
      stopBridge(outflowMirrorBridge);
      stopBridge(outflowAcksBridge);
      stopBridge(targetBridge);
      ackDivert.stop();
      ackReceiver.stop();
    } catch (Exception e) {
      throw new ContinuityException("Unable to stop bridge", e);
    }
    
    isStarted = false;
  }


  private void createAckDivert() throws ContinuityException {
    this.ackDivert = new AckDivert(service, this);
  }
  
  private void createAckReceiver() throws ContinuityException {
    this.ackReceiver = new AckReceiver(service, this);
  }

  private void createAckManager() throws ContinuityException {
    this.ackManager = new AckManager(service, this);
  }

  private void createFlowQueue(final String addressName, final String queueName) throws ContinuityException {
    try {
      if(!queueExists(queueName)) {
        getServer().createQueue(SimpleString.toSimpleString(addressName), 
          RoutingType.MULTICAST,
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

    // TOOO: update to match queue settings
    try {
      if(!queueExists(queueName)) {
        getServer().createQueue(SimpleString.toSimpleString(addressName), 
          RoutingType.MULTICAST,
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
      log.debug("Checking if queue {} exists: {}", queueName, queueSearch.isExists());
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
        put("messageOrigin", getConfig().getSiteId());
      }});

      final DivertConfiguration divert = new DivertConfiguration()
        .setName(divertName)
        .setAddress(sourceAddress)
        .setForwardingAddress(targetAddress)
        .setFilterString("ARTEMIS_MESSAGE_ORIGIN = '" + getConfig().getSiteId() + "' OR ARTEMIS_MESSAGE_ORIGIN IS NULL")
        .setExclusive(false).setTransformerConfiguration(originTransformer);

      getServer().deployDivert(divert);

    } catch (final Exception e) {
      final String eMessage = String.format("Failed to create divert: %s", sourceAddress);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  private Bridge createBridge(final String bridgeName, final String fromQueue, final String toAddress, final String connectorRef, final boolean start) throws ContinuityException {
    Bridge bridge = null;
    try {
      final BridgeConfiguration bridgeConfig = new BridgeConfiguration()
          .setName(bridgeName)
          .setQueueName(fromQueue)
          .setForwardingAddress(toAddress)
          .setHA(true)
          .setRetryInterval(100L)
          .setRetryIntervalMultiplier(0.5)
          .setInitialConnectAttempts(-1)
          .setReconnectAttempts(-1)
          .setUseDuplicateDetection(true)
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

  public AckDivert getAckDivert() {
    return ackDivert;
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
