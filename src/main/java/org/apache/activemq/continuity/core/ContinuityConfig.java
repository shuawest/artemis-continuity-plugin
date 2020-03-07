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
    this.siteId = parseRequiredProperty(properties, "site-id");
    this.localUsername = parseRequiredProperty(properties, "local-username");
    this.localPassword = parseRequiredProperty(properties, "local-password");
    this.remoteUsername = parseRequiredProperty(properties, "remote-username");
    this.remotePassword = parseRequiredProperty(properties, "remote-password");
    this.servingAcceptors = parseRequiredListProperty(properties, "serving-acceptors");
    this.localConnectorRef = parseRequiredProperty(properties, "local-connector-ref");
    this.remoteConnectorRefs = parseRequiredListProperty(properties, "remote-connector-refs");
    this.siteActiveByDefault = parseRequiredBooleanProperty(properties, "active-on-start");

    this.inflowStagingDelay = parseLongProperty(properties, "inflow-staging-delay-ms", 60000L);
    this.bridgeInterval = parseLongProperty(properties, "bridge-interval-ms", 100L);
    this.bridgeIntervalMultiplier = parseDoubleProperty(properties, "bridge-interval-multiplier", 0.5);
    this.outflowExhaustedPollDuration = parseLongProperty(properties, "outflow-exhausted-poll-duration-ms", 100L);
    this.inflowAcksConsumedPollDuration = parseLongProperty(properties, "inflow-acks-consumed-poll-duration-ms", 100L);
    this.activationTimeout = parseLongProperty(properties, "activation-timeout-ms", 300000L); // default to 5mins
    this.isReorgManagementHierarchy = parseBooleanProperty(properties, "reorg-management-hierarchy", true);

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
    if(value == null)
      return new ArrayList<String>();
    else
      return Arrays.asList(value.split(";"));
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
