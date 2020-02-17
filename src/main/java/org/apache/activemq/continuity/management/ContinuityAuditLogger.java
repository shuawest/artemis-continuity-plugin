/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.continuity.management;

import java.security.AccessController;
import java.security.Principal;
import java.util.Arrays;

import javax.security.auth.Subject;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Logger Code 60
 *
 * each message id must be 6 digits long starting with 22, the 3rd digit donates the level so
 *
 * INF0  1
 * WARN  2
 * DEBUG 3
 * ERROR 4
 * TRACE 5
 * FATAL 6
 *
 * so an INFO message would be 601000 to 601999
 */
@MessageLogger(projectCode = "CTY")
public interface ContinuityAuditLogger extends BasicLogger {

    ContinuityAuditLogger LOGGER = Logger.getMessageLogger(ContinuityAuditLogger.class, "org.apache.activemq.audit.base");
    ContinuityAuditLogger MESSAGE_LOGGER = Logger.getMessageLogger(ContinuityAuditLogger.class, "org.apache.activemq.audit.message");
 
    static boolean isEnabled() {
       return LOGGER.isEnabled(Logger.Level.INFO);
    }
 
    static boolean isMessageEnabled() {
       return MESSAGE_LOGGER.isEnabled(Logger.Level.INFO);
    }
 
    static String getCaller() {
       Subject subject = Subject.getSubject(AccessController.getContext());
       String caller = "anonymous";
       if (subject != null) {
          caller = "";
          for (Principal principal : subject.getPrincipals()) {
             caller += principal.getName() + "|";
          }
       }
       return caller;
    }
 
    static String arrayToString(Object value) {
       if (value == null) return "";
 
       final String prefix = "with parameters: ";
 
       if (value instanceof long[]) {
          return prefix + Arrays.toString((long[])value);
       } else if (value instanceof int[]) {
          return prefix + Arrays.toString((int[])value);
       } else if (value instanceof char[]) {
          return prefix + Arrays.toString((char[])value);
       } else if (value instanceof byte[]) {
          return prefix + Arrays.toString((byte[])value);
       } else if (value instanceof float[]) {
          return prefix + Arrays.toString((float[])value);
       } else if (value instanceof short[]) {
          return prefix + Arrays.toString((short[])value);
       } else if (value instanceof double[]) {
          return prefix + Arrays.toString((double[])value);
       } else if (value instanceof boolean[]) {
          return prefix + Arrays.toString((boolean[])value);
       } else if (value instanceof Object[]) {
          return prefix + Arrays.toString((Object[])value);
       } else {
          return prefix + value.toString();
       }
    }

    static void isServiceInitialized(Object source) {
        LOGGER.isServiceInitialized(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221001, value = "User {0} is getting isServiceInitialized on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void isServiceInitialized(String user, Object source, Object... args);

    static void isServiceStarted(Object source) {
        LOGGER.isServiceStarted(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221002, value = "User {0} is getting isServiceStarted on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void isServiceStarted(String user, Object source, Object... args);
    
    static void isCommandManagerInitialized(Object source) {
        LOGGER.isCommandManagerInitialized(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221003, value = "User {0} is getting isCommandManagerInitialized on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void isCommandManagerInitialized(String user, Object source, Object... args);
    
    static void isCommandManagerStarted(Object source) {
        LOGGER.isCommandManagerStarted(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221004, value = "User {0} is getting isCommandManagerStarted on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void isCommandManagerStarted(String user, Object source, Object... args);

    static void getSiteId(Object source) {
        LOGGER.getSiteId(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221005, value = "User {0} is getting getSiteId on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getSiteId(String user, Object source, Object... args);

    static void getLocalInVmUri(Object source) {
        LOGGER.getLocalInVmUri(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221006, value = "User {0} is getting getLocalInVmUri on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getLocalInVmUri(String user, Object source, Object... args);

    static void getLocalUsername(Object source) {
        LOGGER.getLocalUsername(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221007, value = "User {0} is getting getLocalUsername on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getLocalUsername(String user, Object source, Object... args);

    static void getBridgeInterval(Object source) {
        LOGGER.getBridgeInterval(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221008, value = "User {0} is getting getBridgeInterval on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getBridgeInterval(String user, Object source, Object... args);

    static void getBridgeIntervalMultiplier(Object source) {
        LOGGER.getBridgeIntervalMultiplier(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221009, value = "User {0} is getting getBridgeIntervalMultiplier on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getBridgeIntervalMultiplier(String user, Object source, Object... args);

    static void getAddresses(Object source) {
        LOGGER.getAddresses(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221010, value = "User {0} is getting getAddresses on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getAddresses(String user, Object source, Object... args);

    static void getOutflowMirrorSuffix(Object source) {
        LOGGER.getOutflowMirrorSuffix(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221011, value = "User {0} is getting getOutflowMirrorSuffix on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getOutflowMirrorSuffix(String user, Object source, Object... args);

    static void getOutflowAcksSuffix(Object source) {
        LOGGER.getOutflowAcksSuffix(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221012, value = "User {0} is getting getOutflowAcksSuffix on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getOutflowAcksSuffix(String user, Object source, Object... args);

    static void getInflowMirrorSuffix(Object source) {
        LOGGER.getInflowMirrorSuffix(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 2210013, value = "User {0} is getting getInflowMirrorSuffix on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getInflowMirrorSuffix(String user, Object source, Object... args);

    static void getInflowAcksSuffix(Object source) {
        LOGGER.getInflowAcksSuffix(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221014, value = "User {0} is getting getInflowAcksSuffix on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getInflowAcksSuffix(String user, Object source, Object... args);

    static void getInflowStagingDelay(Object source) {
        LOGGER.getInflowStagingDelay(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221015, value = "User {0} is getting getInflowStagingDelay on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getInflowStagingDelay(String user, Object source, Object... args);

    static void getCommandDestinationPrefix(Object source) {
        LOGGER.getCommandDestinationPrefix(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221016, value = "User {0} is getting getCommandDestinationPrefix on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getCommandDestinationPrefix(String user, Object source, Object... args);

    static void getLocalConnector(Object source) {
        LOGGER.getLocalConnector(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221017, value = "User {0} is getting getLocalConnector on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getLocalConnector(String user, Object source, Object... args);

    static void getRemoteConnector(Object source) {
        LOGGER.getRemoteConnector(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221018, value = "User {0} is getting getRemoteConnector on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getRemoteConnector(String user, Object source, Object... args);

    static void startService(Object source) {
        LOGGER.startService(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221019, value = "User {0} is starting continuity service on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void startService(String user, Object source, Object... args);

    static void stopService(Object source) {
        LOGGER.stopService(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221020, value = "User {0} is stopping continuity service on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void stopService(String user, Object source, Object... args);

    static void activateSite(Object source) {
        LOGGER.activateSite(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221021, value = "User {0} is activating site with continuity service on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void activateSite(String user, Object source, Object... args);

    static void deactivateSite(Object source) {
        LOGGER.deactivateSite(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221029, value = "User {0} is deactivating site with continuity service on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void deactivateSite(String user, Object source, Object... args);

    static void initializeCommandManager(Object source) {
        LOGGER.initializeCommandManager(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221022, value = "User {0} is initializing continuity command manager on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void initializeCommandManager(String user, Object source, Object... args);

    static void startCommandManager(Object source) {
        LOGGER.startCommandManager(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221023, value = "User {0} is starting continuity command manager on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void startCommandManager(String user, Object source, Object... args);

    static void stopCommandManager(Object source) {
        LOGGER.stopCommandManager(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221024, value = "User {0} is stopping continuity command manager on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void stopCommandManager(String user, Object source, Object... args);

    static void sendCommand(Object source, Object... args) {
        LOGGER.sendCommand(getCaller(), source, arrayToString(args));
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221025, value = "User {0} is sending continuity command on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void sendCommand(String user, Object source, Object... args);

    static void handleCommand(Object source, Object... args) {
        LOGGER.handleCommand(getCaller(), source, arrayToString(args));
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221026, value = "User {0} is handling continuity command on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void handleCommand(String user, Object source, Object... args);

    /* Flow */

    static void getFlowInitialized(Object source) {
        LOGGER.getFlowInitialized(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221117, value = "User {0} is getting flow isInitialized on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getFlowInitialized(String user, Object source, Object... args);

    static void getFlowStarted(Object source) {
        LOGGER.getFlowStarted(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221118, value = "User {0} is getting flow isStarted on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getFlowStarted(String user, Object source, Object... args);

    static void getSubjectAddressName(Object source) {
        LOGGER.getSubjectAddressName(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221101, value = "User {0} is getting subjectAddressName on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getSubjectAddressName(String user, Object source, Object... args);

    static void getSubjectQueueName(Object source) {
        LOGGER.getSubjectQueueName(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221102, value = "User {0} is getting subjectQueueName on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getSubjectQueueName(String user, Object source, Object... args);

    static void getOutflowMirrorName(Object source) {
        LOGGER.getOutflowMirrorName(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221103, value = "User {0} is getting outflowMirrorName on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getOutflowMirrorName(String user, Object source, Object... args);

    static void getOutflowMirrorBridgeName(Object source) {
        LOGGER.getOutflowMirrorBridgeName(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221104, value = "User {0} is getting outflowMirrorBridgeName on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getOutflowMirrorBridgeName(String user, Object source, Object... args);

    static void getOutflowAcksName(Object source) {
        LOGGER.getOutflowAcksName(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221105, value = "User {0} is getting outflowAcksName on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getOutflowAcksName(String user, Object source, Object... args);

    static void getOutflowAcksBridgeName(Object source) {
        LOGGER.getOutflowAcksBridgeName(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221106, value = "User {0} is getting outflowAcksBridgeName on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getOutflowAcksBridgeName(String user, Object source, Object... args);

    static void getInflowMirrorName(Object source) {
        LOGGER.getInflowMirrorName(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221107, value = "User {0} is getting inflowMirrorName on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getInflowMirrorName(String user, Object source, Object... args);

    static void getInflowAcksName(Object source) {
        LOGGER.getInflowAcksName(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221108, value = "User {0} is getting inflowAcksName on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getInflowAcksName(String user, Object source, Object... args);

    static void getTargetBridgeName(Object source) {
        LOGGER.getTargetBridgeName(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221109, value = "User {0} is getting targetBridgeName on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getTargetBridgeName(String user, Object source, Object... args);

    static void getAverageAckDuration(Object source) {
        LOGGER.getAverageAckDuration(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221119, value = "User {0} is getting averageAckDuration on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getAverageAckDuration(String user, Object source, Object... args);

    static void getMaxAckDuration(Object source) {
        LOGGER.getMaxAckDuration(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221120, value = "User {0} is getting maxAckDuration on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getMaxAckDuration(String user, Object source, Object... args);

    static void getMinAckDuration(Object source) {
        LOGGER.getMinAckDuration(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221121, value = "User {0} is getting minAckDuration on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void getMinAckDuration(String user, Object source, Object... args);


    static void isAddDuplicatesToTarget(Object source) {
        LOGGER.isAddDuplicatesToTarget(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221110, value = "User {0} is getting isAddDuplicatesToTarget on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void isAddDuplicatesToTarget(String user, Object source, Object... args);

    static void isRemoveMessageFromMirror(Object source) {
        LOGGER.isRemoveMessageFromMirror(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221111, value = "User {0} is getting isRemoveMessageFromMirror on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void isRemoveMessageFromMirror(String user, Object source, Object... args);

    static void isDelayMessageOnInflow(Object source) {
        LOGGER.isDelayMessageOnInflow(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221112, value = "User {0} is getting isDelayMessageOnInflow on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void isDelayMessageOnInflow(String user, Object source, Object... args);

    static void handleAck(Object source, Object... args) {
        LOGGER.handleAck(getCaller(), source, arrayToString(args));
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221113, value = "User {0} is handling ack on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void handleAck(String user, Object source, Object... args);

    static void setAddDuplicatesToTarget(Object source, Object... args) {
        LOGGER.setAddDuplicatesToTarget(getCaller(), source, arrayToString(args));
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221114, value = "User {0} is setting isAddDuplicatesToTarget on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void setAddDuplicatesToTarget(String user, Object source, Object... args);

    static void setRemoveMessageFromMirror(Object source, Object... args) {
        LOGGER.setRemoveMessageFromMirror(getCaller(), source, arrayToString(args));
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221115, value = "User {0} is setting isRemoveMessageFromMirror on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void setRemoveMessageFromMirror(String user, Object source, Object... args);

    static void setDelayMessageOnInflow(Object source, Object... args) {
        LOGGER.setDelayMessageOnInflow(getCaller(), source, arrayToString(args));
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221116, value = "User {0} is setting isDelayMessageOnInflow on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void setDelayMessageOnInflow(String user, Object source, Object... args);

   
}
