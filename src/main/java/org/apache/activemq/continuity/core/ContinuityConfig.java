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

  private String siteId; // site ID is annotated on messages to identify origin
  
  private String localInVmUri;
  private String localUsername;
  private String localPassword;

  private String outflowMirrorSuffix;
  private String outflowAcksSuffix;
  private String inflowMirrorSuffix;
  private String inflowAcksSuffix;
  private Long inflowStagingDelay;
  private Long bridgeInterval;
  private Double bridgeIntervalMultiplier;

  private String commandDestinationPrefix;
  private Boolean isReorgManagementHierarchy;
  
  private String localConnectorRef;
  private String remoteConnectorRef;

  public ContinuityConfig(Map<String, String> properties) {
    this.siteId = parseProperty(properties, "site-id");
    this.localInVmUri = parseProperty(properties, "local-invm-uri");
    this.localUsername = parseProperty(properties, "local-username");
    this.localPassword = parseProperty(properties, "local-password");
    this.outflowMirrorSuffix = parseProperty(properties, "outflow-mirror-suffix", DEFAULT_OUTFLOW_MIRROR_SUFFIX);
    this.outflowAcksSuffix = parseProperty(properties, "outflow-acks-suffix", DEFAULT_OUTFLOW_ACKS_SUFFIX);
    this.inflowMirrorSuffix = parseProperty(properties, "inflow-mirror-suffix", DEFAULT_INFLOW_MIRROR_SUFFIX);
    this.inflowAcksSuffix = parseProperty(properties, "inflow-acks-suffix", DEFAULT_INFLOW_ACKS_SUFFIX);
    this.inflowStagingDelay = parseLongProperty(properties, "inflow-staging-delay-ms", 60000L);
    this.bridgeInterval = parseLongProperty(properties, "bridge-interval-ms", 100L);
    this.bridgeIntervalMultiplier = parseDoubleProperty(properties, "bridge-interval-multiplier", 0.5);

    this.commandDestinationPrefix = parseProperty(properties, "command-destination-prefix", DEFAULT_CMD_DESTINATION_PREFIX);
    this.isReorgManagementHierarchy = parseBooleanProperty(properties, "reorg-management-hierarchy", false);
    this.localConnectorRef = parseProperty(properties, "local-connector-ref");
    this.remoteConnectorRef = parseProperty(properties, "remote-connector-ref");

    log.debug("Continuity config parsed: {}", this.toString());
  }

  public String getSiteId() {
    return siteId;
  }

  public String getLocalInVmUri() {
    return localInVmUri;
  }

  public String getLocalUsername() {
    return localUsername;
  }

  public String getLocalPassword() {
    return localPassword;
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

  public String getCommandDestinationPrefix() {
    return commandDestinationPrefix;
  }

  public Boolean isReorgManagmentHierarchy() {
    return isReorgManagementHierarchy;
  }

  public String getLocalConnectorRef() {
    return localConnectorRef;
  }

  public String getRemoteConnectorRef() {
    return remoteConnectorRef;
  }
  
  @Override
  public String toString() {
    return "ContinuityConfig [" + 
      "siteId=" + siteId +
      ", localInVmUri=" + localInVmUri + 
      ", localUsername=" + localUsername +
      ", localPassword=" + ((localPassword == null)? "null" : "******") + 
      ", outMirrorSuffix=" + outflowMirrorSuffix +
      ", outAcksSuffix=" + outflowAcksSuffix +
      ", inMirrorSuffix=" + inflowMirrorSuffix +
      ", inAcksSuffix=" + inflowAcksSuffix +    
      ", inflowStagingDelay=" + inflowStagingDelay +
      ", bridgeInterval=" + bridgeInterval +
      ", bridgeIntervalMultiplier=" + bridgeIntervalMultiplier +
      ", commandDestinationPrefix=" + commandDestinationPrefix +
      ", isReorgManagementHierarchy=" + isReorgManagementHierarchy +
      ", localConnectorRef=" + localConnectorRef + 
      ", remoteConnectorRef=" + remoteConnectorRef + "]";
  }


  private static String parseProperty(Map<String, String> properties, String name, String defaultValue) {
    String value = parseProperty(properties, name);
    return (value != null) ? value : defaultValue;
  }

  private static String parseProperty(Map<String, String> properties, String name) {
    return properties.get(name);
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
