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

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.activemq.artemis.api.core.ActiveMQPropertyConversionException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.Pair;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.filter.Filter;
import org.apache.activemq.artemis.core.filter.impl.FilterImpl;
import org.apache.activemq.artemis.core.postoffice.DuplicateIDCache;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AckManager {

  private static final Logger log = LoggerFactory.getLogger(AckManager.class);

  private final ContinuityService service;
  private final ContinuityFlow flow;

  private boolean isAddDuplicatesToTarget = true;
  private boolean isRemoveMessageFromMirror = true;
  private boolean isDelayMessageOnInflow = true;

  private Long averageAckDuration = null;
  private Long peakAckDuration = null;

  public AckManager(final ContinuityService service, final ContinuityFlow flow) {
    this.service = service;
    this.flow = flow;
  }

  public void handleAck(AckInfo ack) throws ContinuityException {
    if(isAddDuplicatesToTarget) {
      addDuplicateIdToTarget(ack.getMessageUuid());
    }
    if(isRemoveMessageFromMirror) {
    removeMessageFromMirror(flow.getInflowMirrorName(), ack.getMessageUuid());
    }
    updateAckStats(ack);
  }

  public void handleInflowMirrorMessage(Message message) throws ContinuityException {
    if(isDelayMessageOnInflow) {
      delayMessageOnInflowMirror(message);
    }
    // TODO: should we expire after a certain amount of time instead?
    // TODO: otherwise inflow to subjectQueue bridge can be paused until backup becomes active?
  } 

  private void updateAckStats(AckInfo ack) {
    long ackDuration = ack.getAckTime().getTime() - ack.getMessageSendTime().getTime();

    this.peakAckDuration = (peakAckDuration == null || peakAckDuration < ackDuration)? ackDuration : peakAckDuration;
    this.averageAckDuration = (averageAckDuration == null)? ackDuration : (ackDuration + averageAckDuration)/2;

    if(log.isDebugEnabled()) {
      log.debug("Updated ack stats averageAckDuration = {}, peakAckDuration = {}, ackDuration = {}", averageAckDuration, peakAckDuration, ackDuration);
    }
  }

  public void addDuplicateIdToTarget(String duplicateId) throws ContinuityException {
    try 
    { 
      // Add message id to duplicate cache
      byte[] messageIdBytes = SimpleString.toSimpleString(duplicateId).getData();

      if(log.isDebugEnabled())
        log.debug("Adding duplicate ID to mirror id cache for '{}': {}", flow.getSubjectQueueName(), messageIdBytes);

      DuplicateIDCache idCache = getServer().getPostOffice().getDuplicateIDCache(SimpleString.toSimpleString(flow.getSubjectQueueName()));  
      idCache.addToCache(messageIdBytes, null, true); // TODO: instant add - what impact does it have?
 
    } catch (Exception e) {
      String eMessage = String.format("Failed add duplicate id to '%s': %s '", flow.getSubjectQueueName(), duplicateId);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  public void printDupIdCache(String address) throws ContinuityException {
    try 
    {
      // Print dup ID caches
      List<Pair<byte[], Long>> addrIds = getServer().getPostOffice().getDuplicateIDCache(SimpleString.toSimpleString(address)).getMap();
      
      if(log.isDebugEnabled())
        log.debug("Address Ids '{}': {}", address, addrIds);

    } catch (Exception e) {
      String eMessage = "Failed log duplicate id cache for '" + address;
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }
  
  public void removeMessageFromMirror(String queueName, String duplicateId) throws ContinuityException {
    try 
    { 
      // Remove message from mirror based on ack
      String dupIdHeader = Message.HDR_DUPLICATE_DETECTION_ID.toString();
      Filter filter = FilterImpl.createFilter(String.format("%s = '%s'", dupIdHeader, duplicateId));
      Queue mirrorQueue = getServer().locateQueue(SimpleString.toSimpleString(queueName));
      if(mirrorQueue != null)
        mirrorQueue.deleteMatchingReferences(filter);
      else
        throw new ContinuityException(String.format("inflow mirror queue did not exist: %s", queueName));
    } catch (Exception e) {
      String eMessage = String.format("Failed remove duplicates from mirror '%s': %s '", queueName, duplicateId);
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }
  }

  public void delayMessageOnInflowMirror(final Message message) throws ContinuityException {
    if(log.isDebugEnabled())
      log.debug("Trying to delay message on queue '{}'", message.getAddress().toString());
    
    try {
      // Backup original scheduled delivery time on message
      final Long origSchedDeliveryTime = message.getScheduledDeliveryTime();
      message.putLongProperty("continuity-original-sched-delivery-time", origSchedDeliveryTime);
      
      final long messageTimestamp = message.getTimestamp();
      final long currentTime = System.currentTimeMillis();
      final long inflowStagingDelay = getConfig().getInflowStagingDelay();  
      final long scheduledDeliveryTime = (inflowStagingDelay + messageTimestamp);

      if(log.isDebugEnabled()) {
        log.debug("Evaluating message delay - current time '{}', message time '{}'", currentTime, messageTimestamp); 
      
        DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ"); 
        String scheduledDeliveryTimeAsDatetime = datetimeFormat.format(new Date(scheduledDeliveryTime)); 
        log.debug("\nScheduled delivery time '{}', as datetime '{}'", scheduledDeliveryTime, scheduledDeliveryTimeAsDatetime); 
      }

      message.setScheduledDeliveryTime(scheduledDeliveryTime);

    } catch(final ActiveMQPropertyConversionException propException) {
      final String eMessage = "Failed while backing up original scheduled delivery time";
      log.error(eMessage, propException);
      throw new ContinuityException(eMessage, propException);
    } catch(final Exception e) {
      final String eMessage = "Unable to change delivery time of message";
      log.error(eMessage, e);
      throw new ContinuityException(eMessage, e);
    }  
  }

  private ContinuityConfig getConfig() {
    return service.getConfig();
  }

  private ActiveMQServer getServer() {
    return service.getServer();
  }

  /* Ack stats */ 

  public Long getAverageAckDuration() {
    return averageAckDuration;
  }

  public Long getPeakAckDuration() {
    return peakAckDuration;
  }

  /* Manager control */

  public boolean isAddDuplicatesToTarget() {
    return isAddDuplicatesToTarget;
  }

  public void setAddDuplicatesToTarget(boolean isAddDuplicatesToTarget) {
    this.isAddDuplicatesToTarget = isAddDuplicatesToTarget;
  }

  public boolean isRemoveMessageFromMirror() {
    return isRemoveMessageFromMirror;
  }

  public void setRemoveMessageFromMirror(boolean isRemoveMessageFromMirror) {
    this.isRemoveMessageFromMirror = isRemoveMessageFromMirror;
  }

  public boolean isDelayMessageOnInflow() {
    return isDelayMessageOnInflow;
  }

  public void setDelayMessageOnInflow(boolean isDelayMessageOnInflow) {
    this.isDelayMessageOnInflow = isDelayMessageOnInflow;
  }

}
