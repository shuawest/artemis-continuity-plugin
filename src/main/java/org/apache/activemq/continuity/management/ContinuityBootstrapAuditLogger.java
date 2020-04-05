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
@MessageLogger(projectCode = "CTB")
public interface ContinuityBootstrapAuditLogger extends BasicLogger {

    ContinuityBootstrapAuditLogger LOGGER = Logger.getMessageLogger(ContinuityBootstrapAuditLogger.class, "org.apache.activemq.audit.base");
    ContinuityBootstrapAuditLogger MESSAGE_LOGGER = Logger.getMessageLogger(ContinuityBootstrapAuditLogger.class, "org.apache.activemq.audit.message");
 
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

    
    static void isBooted(Object source) {
        LOGGER.isBooted(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221900, value = "User {0} is getting isBooted on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void isBooted(String user, Object source, Object... args);


    static void boot(Object source) {
        LOGGER.boot(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221901, value = "User {0} is calling boot on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void boot(String user, Object source, Object... args);


    static void reboot(Object source) {
        LOGGER.reboot(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221902, value = "User {0} is calling reboot on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void reboot(String user, Object source, Object... args);


    static void destroy(Object source) {
        LOGGER.destroy(getCaller(), source);
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221903, value = "User {0} is calling destroy on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void destroy(String user, Object source, Object... args);


    static void configure(Object source, Object... args) {
        LOGGER.configure(getCaller(), source, arrayToString(args));
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221904, value = "User {0} is calling configure on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void configure(String user, Object source, Object... args);


    static void setSecrets(Object source, Object... args) {
        LOGGER.setSecrets(getCaller(), source, arrayToString(args));
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221905, value = "User {0} is calling setSecrets on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void setSecrets(String user, Object source, Object... args);


    static void tune(Object source, Object... args) {
        LOGGER.tune(getCaller(), source, arrayToString(args));
    }

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 221906, value = "User {0} is calling tune on target resource: {1} {2}", format = Message.Format.MESSAGE_FORMAT)
    void tune(String user, Object source, Object... args);
   
}
