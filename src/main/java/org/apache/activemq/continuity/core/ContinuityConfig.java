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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityConfig {

  private static final Logger log = LoggerFactory.getLogger(ContinuityConfig.class);

  private static final String DEFAULT_OUTFLOW_MIRROR_SUFFIX = ".out.mirror";
  private static final String DEFAULT_OUTFLOW_ACKS_SUFFIX = ".out.acks";
  private static final String DEFAULT_INFLOW_MIRROR_SUFFIX = ".in.mirror";
  private static final String DEFAULT_INFLOW_ACKS_SUFFIX = ".in.acks";
  private static final String DEFAULT_CMD_DESTINATION_PREFIX = "continuity.cmd";

  public static final String CONFIG_SITE_ID = "site-id";
  public static final String CONFIG_LOCAL_USERNAME =  "local-username";
  public static final String CONFIG_LOCAL_PASSWORD =  "local-password";
  public static final String CONFIG_REMOTE_USERNAME =   "remote-username";
  public static final String CONFIG_REMOTE_PASSWORD =   "remote-password";
  public static final String CONFIG_SERVING_ACCEPTORS =  "serving-acceptors";
  public static final String CONFIG_LOCAL_CONNECTOR_REF =  "local-connector-ref";
  public static final String CONFIG_REMOTE_CONNECTOR_REFS =  "remote-connector-refs";
  public static final String CONFIG_ACTIVE_ON_START =  "active-on-start";
  
  public static final String CONFIG_INFLOW_STAGING_DELAY =  "inflow-staging-delay-ms";
  public static final String CONFIG_BRIDGE_INTERVAL =  "bridge-interval-ms";
  public static final String CONFIG_BRIDGE_INTERVAL_MULTIPLIER =  "bridge-interval-multiplier";
  public static final String CONFIG_OUTFLOW_EXHAUSTED_POLL_DURATION =  "outflow-exhausted-poll-duration-ms";
  public static final String CONFIG_INFLOW_ACKS_CONSUMED_POLL_DURATION =  "inflow-acks-consumed-poll-duration-ms";
  public static final String CONFIG_ACTIVATION_TIMEOUT =  "activation-timeout-ms";
  public static final String CONFIG_REORG_MGMT =  "reorg-management-hierarchy";


  private String siteId; 
  
  private String localUsername; 
  private String localPassword;
  private String remoteUsername; 
  private String remotePassword;
  private List<String> servingAcceptors = new ArrayList<String>();
  private boolean siteActiveByDefault;

  private String outflowMirrorSuffix;
  private String outflowAcksSuffix;
  private String inflowMirrorSuffix;
  private String inflowAcksSuffix;
  private Long inflowStagingDelay;
  private Long bridgeInterval;
  private Double bridgeIntervalMultiplier;
  private Long outflowExhaustedPollDuration;
  private Long inflowAcksConsumedPollDuration;
  private Long activationTimeout;

  private String commandDestinationPrefix;
  private Boolean isReorgManagementHierarchy;
  
  private String localConnectorRef;
  private List<String> remoteConnectorRefs;

  public ContinuityConfig(Map<String, String> properties) throws ContinuityException {
    this.siteId = parseRequiredProperty(properties, CONFIG_SITE_ID);
    this.localUsername = parseRequiredProperty(properties, CONFIG_LOCAL_USERNAME);
    this.localPassword = parseRequiredProperty(properties, CONFIG_LOCAL_PASSWORD);
    this.remoteUsername = parseRequiredProperty(properties, CONFIG_REMOTE_USERNAME);
    this.remotePassword = parseRequiredProperty(properties, CONFIG_REMOTE_PASSWORD);
    this.servingAcceptors = parseRequiredListProperty(properties, CONFIG_SERVING_ACCEPTORS);
    this.localConnectorRef = parseRequiredProperty(properties, CONFIG_LOCAL_CONNECTOR_REF);
    this.remoteConnectorRefs = parseRequiredListProperty(properties, CONFIG_REMOTE_CONNECTOR_REFS);
    this.siteActiveByDefault = parseRequiredBooleanProperty(properties, CONFIG_ACTIVE_ON_START);

    this.inflowStagingDelay = parseLongProperty(properties, CONFIG_INFLOW_STAGING_DELAY, 60000L);
    this.bridgeInterval = parseLongProperty(properties, CONFIG_BRIDGE_INTERVAL, 1000L);
    this.bridgeIntervalMultiplier = parseDoubleProperty(properties, CONFIG_BRIDGE_INTERVAL_MULTIPLIER, 0.5);
    this.outflowExhaustedPollDuration = parseLongProperty(properties, CONFIG_OUTFLOW_EXHAUSTED_POLL_DURATION, 100L);
    this.inflowAcksConsumedPollDuration = parseLongProperty(properties, CONFIG_INFLOW_ACKS_CONSUMED_POLL_DURATION, 100L);
    this.activationTimeout = parseLongProperty(properties, CONFIG_ACTIVATION_TIMEOUT, 300000L); // 5min default 
    this.isReorgManagementHierarchy = parseBooleanProperty(properties, CONFIG_REORG_MGMT, true);

    this.outflowMirrorSuffix = parseProperty(properties, "outflow-mirror-suffix", DEFAULT_OUTFLOW_MIRROR_SUFFIX);
    this.outflowAcksSuffix = parseProperty(properties, "outflow-acks-suffix", DEFAULT_OUTFLOW_ACKS_SUFFIX);
    this.inflowMirrorSuffix = parseProperty(properties, "inflow-mirror-suffix", DEFAULT_INFLOW_MIRROR_SUFFIX);
    this.inflowAcksSuffix = parseProperty(properties, "inflow-acks-suffix", DEFAULT_INFLOW_ACKS_SUFFIX);
    this.commandDestinationPrefix = parseProperty(properties, "command-destination-prefix", DEFAULT_CMD_DESTINATION_PREFIX);

    log.debug("Continuity config parsed: {}", this.toString());
  }

  public String getSiteId() {
    return siteId;
  }

  public String getLocalUsername() {
    return localUsername;
  }

  public String getLocalPassword() {
    return localPassword;
  }

  public String getRemoteUsername() {
    return remoteUsername;
  }

  public String getRemotePassword() {
    return remotePassword;
  }

  public List<String> getServingAcceptors() {
    return servingAcceptors;
  }

  public String getLocalConnectorRef() {
    return localConnectorRef;
  }

  public List<String> getRemoteConnectorRefs() {
    return remoteConnectorRefs;
  }

  public boolean isSiteActiveByDefault() {
    return siteActiveByDefault;
  }

  public String getOutflowMirrorSuffix() {
    return outflowMirrorSuffix;
  }

  public String getOutflowAcksSuffix() {
    return outflowAcksSuffix;
  }

  public String getInflowMirrorSuffix() {
    return inflowMirrorSuffix;
  }

  public String getInflowAcksSuffix() {
    return inflowAcksSuffix;
  }

  public Long getInflowStagingDelay() {
    return inflowStagingDelay;
  }

  public Long getBridgeInterval() {
    return bridgeInterval;
  }

  public Double getBridgeIntervalMultiplier() {
    return bridgeIntervalMultiplier;
  }

  public Long getOutflowExhaustedPollDuration() {
    return outflowExhaustedPollDuration;
  }

  public Long getInflowAcksConsumedPollDuration() {
    return inflowAcksConsumedPollDuration;
  }

  public Long getActivationTimeout() {
    return activationTimeout;
  }

  public String getCommandDestinationPrefix() {
    return commandDestinationPrefix;
  }

  public Boolean isReorgManagmentHierarchy() {
    return isReorgManagementHierarchy;
  }
  
  @Override
  public String toString() {
    return "ContinuityConfig [" + 
      "siteId=" + siteId +
      ", localUsername=" + localUsername +
      ", localPassword=" + ((localPassword == null)? "null" : "******") + 
      ", remoteUsername=" + remoteUsername +
      ", remotePassword=" + ((remotePassword == null)? "null" : "******") + 
      ", localConnectorRef=" + localConnectorRef + 
      ", remoteConnectorRefs=" + remoteConnectorRefs + 
      ", servingAcceptors=" + servingAcceptors +
      ", siteActiveByDefault=" + siteActiveByDefault +
      ", outMirrorSuffix=" + outflowMirrorSuffix +
      ", outAcksSuffix=" + outflowAcksSuffix +
      ", inMirrorSuffix=" + inflowMirrorSuffix +
      ", inAcksSuffix=" + inflowAcksSuffix +    
      ", inflowStagingDelay=" + inflowStagingDelay +
      ", bridgeInterval=" + bridgeInterval +
      ", bridgeIntervalMultiplier=" + bridgeIntervalMultiplier +
      ", outflowExhaustedPollDuration=" + outflowExhaustedPollDuration +
      ", inflowAcksConsumedPollDuration=" + inflowAcksConsumedPollDuration +
      ", activationTimeout=" + activationTimeout +
      ", commandDestinationPrefix=" + commandDestinationPrefix +
      ", isReorgManagementHierarchy=" + isReorgManagementHierarchy + "]";
  }

  private static String parseRequiredProperty(Map<String, String> properties, String name) throws ContinuityException {
    String value = parseProperty(properties, name);
    if(value == null || value.trim() == "") {
      String msg = String.format("Required continuity configuration property '%s' not set", name);
      throw new ContinuityException(msg);
    }
    return value;
  }

  private static String parseProperty(Map<String, String> properties, String name, String defaultValue) {
    String value = parseProperty(properties, name);
    return (value != null) ? value : defaultValue;
  }

  private static String parseProperty(Map<String, String> properties, String name) {
    return properties.get(name);
  }

  private static List<String> parseRequiredListProperty(Map<String, String> properties, String name) throws ContinuityException {
    List<String> listValue = parseListProperty(properties, name);
    if(listValue.size() == 0) {
      String msg = String.format("Required continuity configuration property '%s' not set", name);
      throw new ContinuityException(msg);
    } else {
      return listValue;
    }
  }

  private static List<String> parseListProperty(Map<String, String> properties, String name) {
    String value = properties.get(name);
    if(value == null || value.trim() == "")
      return new ArrayList<String>();
    else {
      if(value.contains(";"))
        return Arrays.asList(value.split(";"));
      else 
        return Arrays.asList(value.split(","));
    }
  }

  private static Long parseLongProperty(Map<String, String> properties, String name, long defaultValue) {
    Long longValue = parseLongProperty(properties, name);
    return (longValue != null) ? longValue : defaultValue;
  }

  private static Long parseLongProperty(Map<String, String> properties, String name) {
    String value = properties.get(name);
    Long longValue = (value == null)? null : Long.parseLong(value);
    return longValue;
  }

  private static Double parseDoubleProperty(Map<String, String> properties, String name, double defaultValue) {
    Double doubleValue = parseDoubleProperty(properties, name);
    return (doubleValue != null) ? doubleValue : defaultValue;
  }

  private static Double parseDoubleProperty(Map<String, String> properties, String name) {
    String value = properties.get(name);
    Double doubleValue = (value == null)? null : Double.parseDouble(value);
    return doubleValue;
  }

  private static Boolean parseRequiredBooleanProperty(Map<String, String> properties, String name) throws ContinuityException {
    Boolean value = parseBooleanProperty(properties, name);
    if(value == null) {
      String msg = String.format("Required continuity configuration property '%s' not set", name);
      throw new ContinuityException(msg);
    }
    return value;
  }

  private static Boolean parseBooleanProperty(Map<String, String> properties, String name, boolean defaultValue) {
    Boolean boolValue = parseBooleanProperty(properties, name);
    return (boolValue != null) ? boolValue : defaultValue;
  }

  private static Boolean parseBooleanProperty(Map<String, String> properties, String name) {
    String value = properties.get(name);
    Boolean boolValue = (value == null)? null : Boolean.parseBoolean(value);
    return boolValue;
  }

}
