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
  private static final String DEFAULT_CMD_DESTINATION = "artemis.continuity.commands";

  private String siteId; // site ID is annotated on messages to identify origin
  
  private String localInVmUri;
  private String localUsername;
  private String localPassword;

  private String outflowMirrorSuffix;
  private String outflowAcksSuffix;
  private String inflowMirrorSuffix;
  private String inflowAcksSuffix;
  private Long inflowStagingDelay;

  private String commandDestination;
  
  private String localConnectorRef;
  private String remoteConnectorRef;

  private List<String> addresses;

  public ContinuityConfig(Map<String, String> properties) {
    this.siteId = parseProperty(properties, "site-id");
    this.localInVmUri = parseProperty(properties, "local-invm-uri");
    this.localUsername = parseProperty(properties, "local-username");
    this.localPassword = parseProperty(properties, "local-password");
    this.outflowMirrorSuffix = parseProperty(properties, "outflow-mirror-suffix", ".out.mirror");
    this.outflowAcksSuffix = parseProperty(properties, "outflow-acks-suffix", ".out.acks");
    this.inflowMirrorSuffix = parseProperty(properties, "inflow-mirror-suffix", ".in.mirror");
    this.inflowAcksSuffix = parseProperty(properties, "inflow-acks-suffix", ".in.acks");
    this.inflowStagingDelay = parseLongProperty(properties, "inflow-staging-delay-ms", 60000L);
    this.commandDestination = parseProperty(properties, "command-destination", "artemis.continuity.commands");
    this.localConnectorRef = parseProperty(properties, "local-connector-ref");
    this.remoteConnectorRef = parseProperty(properties, "remote-connector-ref");

    String addressesString = parseProperty(properties, "addresses");
    this.addresses = (addressesString != null)? Arrays.asList(addressesString.split(";")) : new ArrayList<String>();

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

  public String getCommandDestination() {
    return commandDestination;
  }

  public String getLocalConnectorRef() {
    return localConnectorRef;
  }

  public String getRemoteConnectorRef() {
    return remoteConnectorRef;
  }

  public List<String> getAddresses() {
    return addresses;
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
      ", localConnectorRef=" + localConnectorRef + 
      ", remoteConnectorRef=" + remoteConnectorRef + 
      ", addresses=" + addresses + "]";
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
    Long longValue = Long.parseLong(value);
    return longValue;
  }
}
