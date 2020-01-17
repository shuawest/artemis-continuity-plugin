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
package org.apache.activemq.continuity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.FileDeploymentManager;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.apache.activemq.artemis.spi.core.security.jaas.InVMLoginModule;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.continuity.core.CommandHandler;
import org.apache.activemq.continuity.core.CommandManager;
import org.apache.activemq.continuity.core.ContinuityConfig;
import org.apache.activemq.continuity.core.ContinuityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityTestBase extends ActiveMQTestBase {

  private static final Logger log = LoggerFactory.getLogger(ContinuityTestBase.class);

  public ServerContext createServerContext(String serverConfigFile, String serverId, String user, String pass)
      throws Exception {
    FileConfiguration fc = new FileConfiguration();
    FileDeploymentManager deploymentManager = new FileDeploymentManager(serverConfigFile);
    deploymentManager.addDeployable(fc);
    deploymentManager.readConfiguration();

    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    ActiveMQJAASSecurityManager securityManager = new ActiveMQJAASSecurityManager(InVMLoginModule.class.getName(),
        new SecurityConfiguration());
    securityManager.getConfiguration().addUser(user, pass);
    securityManager.getConfiguration().addRole(user, "amq");

    ActiveMQServer server = addServer(ActiveMQServers.newActiveMQServer(fc, mbeanServer, securityManager, false));
    server.setIdentity(serverId);

    return new ServerContext(server, mbeanServer, fc, securityManager);
  }

  public ContinuityContext createMockContext(ServerContext serverContext, String serverId, int inVmAcceptorId) {
    ContinuityService serviceMock = mock(ContinuityService.class);
    ContinuityConfig configMock = mock(ContinuityConfig.class);
    CommandManager commandManagerMock = mock(CommandManager.class);
    CommandHandler commandHandlerMock = mock(CommandHandler.class);

    when(serviceMock.getServer()).thenReturn(serverContext.getServer());
    when(serviceMock.getConfig()).thenReturn(configMock);
    when(configMock.getSiteId()).thenReturn(serverId);
    when(configMock.getCommandDestinationPrefix()).thenReturn("artemis.continuity.commands");
    when(configMock.getLocalInVmUri()).thenReturn("vm://" + inVmAcceptorId);
    when(configMock.getLocalUsername()).thenReturn("myuser");
    when(configMock.getLocalPassword()).thenReturn("mypass");
    // when(configMock.getLocalConnectorRef()).thenReturn("local-connector");
    when(configMock.getRemoteConnectorRef()).thenReturn("remote-connector");

    ContinuityContext continuityCtx = new ContinuityContext();
    continuityCtx.setConfig(configMock);
    continuityCtx.setService(serviceMock);
    continuityCtx.setCommandManager(commandManagerMock);
    continuityCtx.setCommandHandler(commandHandlerMock);
    return continuityCtx;
  }

  public void produceAndConsumeMessage(ContinuityConfig continuityConfig, ServerContext serverCtx, 
                                       String address, String queueName, MessageHandler handler, 
                                       String messageBody, String dupId) throws Exception {

    ServerLocator locator = ActiveMQClient.createServerLocator(continuityConfig.getLocalInVmUri());
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(continuityConfig.getLocalUsername(),
      continuityConfig.getLocalPassword(), false, true, true, false, locator.getAckBatchSize());
    
    ClientProducer producer = session.createProducer(address);
    ClientConsumer consumer = session.createConsumer(queueName);
    consumer.setMessageHandler(handler);

    session.start();

    ClientMessage msg = session.createMessage(true);
    msg.getBodyBuffer().writeString(messageBody);

    if (dupId != null)
      msg.putStringProperty(Message.HDR_DUPLICATE_DETECTION_ID, dupId);

    if (log.isDebugEnabled()) {
      log.debug("sending message (dupId: {}): {}", dupId, messageBody);
    }

    producer.send(msg);

    Thread.sleep(100L);

    consumer.close();
    producer.close();
    session.close();
    factory.close();
    locator.close();
  }

  public void produceMessage(ContinuityConfig continuityConfig, ServerContext serverCtx, 
                             String address, String queueName, 
                             String messageBody, String dupId) throws Exception {  

    ServerLocator locator = ActiveMQClient.createServerLocator(continuityConfig.getLocalInVmUri());
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(continuityConfig.getLocalUsername(), continuityConfig.getLocalPassword(), false, true, true, false, locator.getAckBatchSize());;
    ClientProducer producer = session.createProducer(address);
   
    session.start();
    
    ClientMessage msg = session.createMessage(true);
    msg.getBodyBuffer().writeString(messageBody);
  
    if(dupId != null)
      msg.putStringProperty(Message.HDR_DUPLICATE_DETECTION_ID, dupId);
      
    producer.send(msg);

    producer.close();
    session.close();
    factory.close();
    locator.close();
  }

  public void consumeMessages(ContinuityConfig continuityConfig, ServerContext serverCtx, String address, String queueName, MessageHandler handler) throws Exception {

    ServerLocator locator = ActiveMQClient.createServerLocator(continuityConfig.getLocalInVmUri());
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(continuityConfig.getLocalUsername(),
      continuityConfig.getLocalPassword(), false, true, true, false, locator.getAckBatchSize());
  
    ClientConsumer consumer = session.createConsumer(queueName);
    consumer.setMessageHandler(handler);

    session.start();
    Thread.sleep(100L);

    consumer.close();
    session.close();
    factory.close();
    locator.close();
  }

  public class MessageHandlerStub implements MessageHandler {    
    @Override
    public void onMessage(ClientMessage message) {
      if (log.isDebugEnabled()) {
        String body = message.getBodyBuffer().readString();
        log.debug("Received message: {}", body);
      }

      try {
        message.acknowledge();
      } catch (ActiveMQException e) {
        log.error("Unable to acknowledge message", e);
      }
    }
  }

  protected class ServerContext {
    private ActiveMQServer server;
    private MBeanServer mbeanServer;
    private Configuration brokerConfig;
    private ActiveMQSecurityManager securityManager;

    public ServerContext(ActiveMQServer server, MBeanServer mbeanServer, Configuration brokerConfig, ActiveMQSecurityManager securityManager) {
      this.server = server;
      this.mbeanServer = mbeanServer;
      this.brokerConfig = brokerConfig;
      this.securityManager = securityManager;
    }

    public ActiveMQServer getServer() {
      return server;
    }
    public MBeanServer getMbeanServer() {
      return mbeanServer;
    }
    public Configuration getBrokerConfig() {
      return brokerConfig;
    }
    public ActiveMQSecurityManager getSecurityManager() {
      return securityManager;
    }
  }

  protected class ContinuityContext {
    private ContinuityConfig config;
    private ContinuityService service;
    private CommandManager commandManager;
    private CommandHandler commandHandler;
    
    public ContinuityContext() {}

    public ContinuityConfig getConfig() {
      return config;
    }
    public void setConfig(ContinuityConfig config) {
      this.config = config;
    }
    public ContinuityService getService() {
      return service;
    }
    public void setService(ContinuityService service) {
      this.service = service;
    }
    public CommandManager getCommandManager() {
      return commandManager;
    }
    public void setCommandManager(CommandManager commandManager) {
      this.commandManager = commandManager;
    }
    public CommandHandler getCommandHandler() {
      return commandHandler;
    }
    public void setCommandHandler(CommandHandler commandHandler) {
      this.commandHandler = commandHandler;
    }
  }
}
