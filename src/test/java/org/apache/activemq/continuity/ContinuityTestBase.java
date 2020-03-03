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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.management.MBeanServer;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.FileDeploymentManager;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQScheduledComponent;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerBasePlugin;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.apache.activemq.artemis.spi.core.security.jaas.InVMLoginModule;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.continuity.core.CommandManager;
import org.apache.activemq.continuity.core.CommandReceiver;
import org.apache.activemq.continuity.core.ContinuityConfig;
import org.apache.activemq.continuity.core.ContinuityService;
import org.apache.activemq.continuity.management.ContinuityManagementService;
import org.apache.activemq.continuity.plugins.ContinuityPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityTestBase extends ActiveMQTestBase {

  private static final Logger log = LoggerFactory.getLogger(ContinuityTestBase.class);

  public ServerContext createServerContext(String serverConfigFile, String jmxDomain, String user, String pass)
      throws Exception {
    FileConfiguration fc = new FileConfiguration();
    fc.setJMXDomain(jmxDomain);

    FileDeploymentManager deploymentManager = new FileDeploymentManager(serverConfigFile);
    deploymentManager.addDeployable(fc);
    deploymentManager.readConfiguration();

    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    ActiveMQJAASSecurityManager securityManager = new ActiveMQJAASSecurityManager(InVMLoginModule.class.getName(),
        new SecurityConfiguration());
    securityManager.getConfiguration().addUser(user, pass);
    securityManager.getConfiguration().addRole(user, "amq");

    ActiveMQServer server = addServer(ActiveMQServers.newActiveMQServer(fc, mbeanServer, securityManager, false));

    return new ServerContext(server, mbeanServer, fc, securityManager);
  }

  public ContinuityContext createMockContext(ServerContext serverContext, String serverId, int inVmAcceptorId) {
    ContinuityService serviceMock = mock(ContinuityService.class);
    ContinuityConfig configMock = mock(ContinuityConfig.class);
    CommandManager commandManagerMock = mock(CommandManager.class);
    CommandReceiver commandRecieverMock = mock(CommandReceiver.class);
    ContinuityManagementService managementMock = mock(ContinuityManagementService.class);

    when(serviceMock.getServer()).thenReturn(serverContext.getServer());
    when(serviceMock.getManagement()).thenReturn(managementMock);
    when(serviceMock.getConfig()).thenReturn(configMock);
    when(configMock.getSiteId()).thenReturn(serverId);
    when(configMock.getCommandDestinationPrefix()).thenReturn("artemis.continuity.commands");
    when(configMock.getLocalUsername()).thenReturn("myuser");
    when(configMock.getLocalPassword()).thenReturn("mypass");
    when(configMock.getRemoteUsername()).thenReturn("myuser");
    when(configMock.getRemotePassword()).thenReturn("mypass");
    when(configMock.getLocalConnectorRef()).thenReturn("local-connector");
    when(configMock.getRemoteConnectorRef()).thenReturn("remote-connector");
    when(configMock.getBridgeInterval()).thenReturn(500L);
    when(configMock.getBridgeIntervalMultiplier()).thenReturn(0.5);

    TransportConfiguration localConnector = serverContext.getServer().getConfiguration().getConnectorConfigurations().get(configMock.getLocalConnectorRef());
    when(serviceMock.getLocalConnector()).thenReturn(localConnector);
    TransportConfiguration remoteConnector = serverContext.getServer().getConfiguration().getConnectorConfigurations().get(configMock.getLocalConnectorRef());
    when(serviceMock.getRemoteConnector()).thenReturn(remoteConnector);

    ContinuityContext continuityCtx = new ContinuityContext();
    continuityCtx.setConfig(configMock);
    continuityCtx.setService(serviceMock);
    continuityCtx.setCommandManager(commandManagerMock);
    continuityCtx.setCommandReceiver(commandRecieverMock);
    return continuityCtx;
  }

  public ContinuityPlugin getContinuityPlugin(ServerContext serverCtx) {
    ContinuityPlugin plugin = null;
    for(ActiveMQServerBasePlugin p : serverCtx.getServer().getBrokerPlugins()) {
      if(p.getClass().equals(ContinuityPlugin.class)) {
        plugin = (ContinuityPlugin)p;
        break;
      }
    }
    return plugin;
  }

  public void produceAndConsumeMessage(ContinuityConfig continuityConfig, ServerContext serverCtx, 
                                       String address, String queueName, MessageHandler handler, 
                                       String messageBody, String dupId) throws Exception {

    TransportConfiguration localConnector = serverCtx.getServer().getConfiguration().getConnectorConfigurations().get(continuityConfig.getLocalConnectorRef());                                        
    ServerLocator locator = ActiveMQClient.createServerLocator(false, localConnector);
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(continuityConfig.getLocalUsername(),
      continuityConfig.getLocalPassword(), false, true, true, true, locator.getAckBatchSize());
    
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

    CoreHandle handle = new CoreHandle(locator, factory, session, producer, consumer);
    handle.close();
  }

  public void produceMessage(ContinuityConfig continuityConfig, ServerContext serverCtx, 
                             String address, String queueName, 
                             String messageBody, String dupId) throws Exception {  

    TransportConfiguration localConnector = serverCtx.getServer().getConfiguration().getConnectorConfigurations().get(continuityConfig.getLocalConnectorRef());                                        
    ServerLocator locator = ActiveMQClient.createServerLocator(false, localConnector);
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(continuityConfig.getLocalUsername(), continuityConfig.getLocalPassword(), false, true, true, false, locator.getAckBatchSize());;
    ClientProducer producer = session.createProducer(address);
   
    session.start();
    
    ClientMessage msg = session.createMessage(true);
    msg.getBodyBuffer().writeString(messageBody);
  
    if(dupId != null)
      msg.putStringProperty(Message.HDR_DUPLICATE_DETECTION_ID, dupId);
      
    producer.send(msg);

    CoreHandle handle = new CoreHandle(locator, factory, session, producer, null);
    handle.close();
  }

  public void produceMessage(String url, String username, String password, String address, String messageBody) throws Exception {  
    ServerLocator locator = ActiveMQClient.createServerLocator(url);
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(username, password, false, true, true, false, locator.getAckBatchSize());;
    ClientProducer producer = session.createProducer(address);
   
    session.start();
    
    ClientMessage msg = session.createMessage(true);
    msg.getBodyBuffer().writeString(messageBody);
     
    producer.send(msg);

    CoreHandle handle = new CoreHandle(locator, factory, session, producer, null);
    handle.close();
  }

  public void consumeMessages(ContinuityConfig continuityConfig, ServerContext serverCtx, String address, String queueName, MessageHandler handler) throws Exception {
    TransportConfiguration localConnector = serverCtx.getServer().getConfiguration().getConnectorConfigurations().get(continuityConfig.getLocalConnectorRef());                                        
    ServerLocator locator = ActiveMQClient.createServerLocator(false, localConnector);
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(continuityConfig.getLocalUsername(),
      continuityConfig.getLocalPassword(), false, true, true, false, locator.getAckBatchSize());
  
    ClientConsumer consumer = session.createConsumer(queueName);
    consumer.setMessageHandler(handler);

    session.start();
    Thread.sleep(100L);

    CoreHandle handle = new CoreHandle(locator, factory, session, null, consumer);
    handle.close();
  }

  public void produceMessages(String uri, String username, String password, String address, String messageBody, int count) throws Exception {
    ServerLocator locator = ActiveMQClient.createServerLocator(uri);
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(username, password, false, true, true, true, locator.getAckBatchSize());
    ClientProducer producer = session.createProducer(address);
    session.start();

    for(int i=0; i < count; i++) {
      ClientMessage msg = session.createMessage(true);
      msg.getBodyBuffer().writeString(messageBody + " " + i);
      producer.send(msg);
    }

    CoreHandle handle = new CoreHandle(locator, factory, session, producer, null);
    handle.close();
  }

  public void consumeMessages(ServerContext serverCtx, String inVmUri, String username, String password,
      String queueName, MessageHandler handler) throws Exception {

    ServerLocator locator = ActiveMQClient.createServerLocator(inVmUri);
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(username, password, false, true, true, true,
        locator.getAckBatchSize());

    ClientConsumer consumer = session.createConsumer(queueName);
    consumer.setMessageHandler(handler);

    session.start();
    Thread.sleep(100L);

    CoreHandle handle = new CoreHandle(locator, factory, session, null, consumer);
    handle.close();
  }

  public void produceJmsMessages(String uri, String username, String password, String address, String messageBody, int count) throws Exception {
    Destination dest = ActiveMQJMSClient.createTopic(address);
    ConnectionFactory factory = new ActiveMQConnectionFactory(uri);
    Connection connection = factory.createConnection(username, password);
    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    connection.start();
    MessageProducer producer = session.createProducer(dest);
    Thread.sleep(500);

    for(int i=0; i < count; i++) {
      TextMessage message = session.createTextMessage(messageBody + " " + i);
      producer.send(message);
    }
    
    JmsHandle handle = new JmsHandle(factory, connection, session, null, producer, null);
    handle.close();
  }

  public CoreHandle startConsumer(ServerContext serverCtx, String inVmUri, String username, String password, String queueName, MessageHandler handler) throws Exception {
    ServerLocator locator = ActiveMQClient.createServerLocator(inVmUri);
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(username, password, false, true, true, true, locator.getAckBatchSize());

    ClientConsumer consumer = session.createConsumer(queueName, "AMQDurable = 'DURABLE'");
    consumer.setMessageHandler(handler);

    return new CoreHandle(locator, factory, session, null, consumer);
  }

  public CoreHandle startCoreConsumer(String url, String username, String password, String queueName, CoreMessageHandlerStub handler) throws Exception {
    ServerLocator locator = ActiveMQClient.createServerLocator(url);
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(username, password, false, false, false, false, 1); // locator.getAckBatchSize());
    handler.setSession(session);

    ClientConsumer consumer = session.createConsumer(queueName, "AMQDurable = 'DURABLE'");
    consumer.setMessageHandler(handler);

    session.start();

    return new CoreHandle(locator, factory, session, null, consumer);
  }

  public JmsHandle startJmsConsumer(String uri, String username, String password, String queueName, JmsMessageListenerStub listener) throws Exception {
    ConnectionFactory factory = new ActiveMQConnectionFactory(uri);
    Connection connection = factory.createConnection(username, password);
    Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
    Queue queue = session.createQueue(queueName);
    
    MessageConsumer consumer = session.createConsumer(queue);
    consumer.setMessageListener(listener);

    listener.setConnection(connection);

    connection.start();

    return new JmsHandle(factory, connection, session, queue, null, consumer);
  }

  public CoreHandle consumeDirect(String url, String username, String password, String address, RoutingType routingType, String queueName, MessageHandler handler) throws Exception {
    ServerLocator locator = ActiveMQClient.createServerLocator(url);
    ClientSessionFactory factory = locator.createSessionFactory();
    ClientSession session = factory.createSession(username, password, false, true, true, false, locator.getAckBatchSize());
  
    session.createQueue(address, routingType, queueName);

    ClientConsumer consumer = session.createConsumer(queueName);
    consumer.setMessageHandler(handler);

    session.start();

    return new CoreHandle(locator, factory, session, null, consumer);
  }


  public ScheduledProducerExecutor startScheduledProducer(ActiveMQServer server, String url, String username, String password, String address, long msPeriod) {
    ScheduledProducerExecutor executor = 
      new ScheduledProducerExecutor(url, username, password, address, 
                                    server.getScheduledPool(), 
                                    server.getExecutorFactory().getExecutor(), 
                                    msPeriod);
    executor.start();
    return executor;
  }

  public ScheduledConsumerExecutor startScheduledConsumer(ActiveMQServer server, String url, String username, String password, String queueName, long msPeriod) {
    ScheduledConsumerExecutor executor = 
      new ScheduledConsumerExecutor(url, username, password, queueName, 
                                    server.getScheduledPool(), 
                                    server.getExecutorFactory().getExecutor(), 
                                    msPeriod);
    executor.start();
    return executor;
  }
  public final class ScheduledProducerExecutor extends ActiveMQScheduledComponent {

    private final String url;
    private final String username;
    private final String password;
    private final String address;

    private CoreHandle coreHandle;
    private long producedCount;

    public ScheduledProducerExecutor(String url, String username, String password, String address, 
                                     ScheduledExecutorService scheduledExecutorService, Executor executor, long checkPeriod) {
      super(scheduledExecutorService, executor, checkPeriod, TimeUnit.MILLISECONDS, false);
      this.url = url;
      this.username = username;
      this.password = password;
      this.address = address;
    }

    @Override
    public void run() {
      try {
        if (coreHandle == null) {
          ServerLocator locator = ActiveMQClient.createServerLocator(url);
          ClientSessionFactory factory = locator.createSessionFactory();
          ClientSession session = factory.createSession(username, password, false, true, true, false, locator.getAckBatchSize());;
          ClientProducer producer = session.createProducer(address);
          session.start();

          coreHandle = new CoreHandle(locator, factory, session, producer, null);
        }

        String messageBody = "Test message " + producedCount;
        ClientMessage msg = coreHandle.getSession().createMessage(true);
        msg.getBodyBuffer().writeString(messageBody);
      
        coreHandle.getProducer().send(msg);

        producedCount++;
      } catch (Exception e) {
        log.error("Unable to produce test message", e);
      }
    }

    @Override
    public void stop() {
      if(coreHandle != null) {
        try {
          coreHandle.close();
        } catch (ActiveMQException e) {
          log.error("Unable to stop scheduled producer", e);
        }
      }

      super.stop();
    }
    
    public long getProducedCount() {
      return producedCount;
    }
  }

  public final class ScheduledConsumerExecutor extends ActiveMQScheduledComponent {

    private final String url;
    private final String username;
    private final String password;
    private final String queue;

    private CoreHandle coreHandle;
    private long consumedCount = 0;

    public ScheduledConsumerExecutor(String url, String username, String password, String queue,
                                     ScheduledExecutorService scheduledExecutorService, Executor executor, long checkPeriod) {
      super(scheduledExecutorService, executor, checkPeriod, TimeUnit.MILLISECONDS, false);
      this.url = url;
      this.username = username;
      this.password = password;
      this.queue = queue;
    }

    @Override
    public void run() {
      try {
        if (coreHandle == null) {
          ServerLocator locator = ActiveMQClient.createServerLocator(url);
          ClientSessionFactory factory = locator.createSessionFactory();
          ClientSession session = factory.createSession(username, password, false, true, true, false, 1);;
          ClientConsumer consumer = session.createConsumer(SimpleString.toSimpleString(queue));
          session.start();
          coreHandle = new CoreHandle(locator, factory, session, null, consumer);
        }

        ClientMessage message = coreHandle.getConsumer().receive();
        message.acknowledge();
        //coreHandle.getSession().commit();
        consumedCount++;

        log.debug("Consumed and acked message: {}", message.getBodyBuffer().readString());

      } catch (Exception e) {
        log.error("Unable to consume test message", e);
      } 
    }

    @Override
    public void stop() {
      if(coreHandle != null) {
        try {
          coreHandle.close();
        } catch (ActiveMQException e) {
          log.error("Unable to stop scheduled producer", e);
        }
      }

      super.stop();
    }

    public long getConsumedCount() {
      return consumedCount;
    }
  }

  public class JmsHandle {
    private final ConnectionFactory factory;
    private final Connection connection;
    private final Session session;
    private final Queue queue;
    private MessageConsumer consumer = null;
    private MessageProducer producer = null;
    public JmsHandle(final ConnectionFactory factory, final Connection connection, final Session session, final Queue queue, MessageProducer producer, MessageConsumer consumer) {
      this.factory = factory;
      this.connection = connection;
      this.session = session;
      this.queue = queue;
      this.producer = producer;
      this.consumer = consumer;
    }
    public void close() throws JMSException {
      if(consumer != null)
        consumer.close();
      if(producer != null)
        producer.close();
      session.close();
      connection.close();
    }
    public ConnectionFactory getFactory() {
      return factory;
    }
    public Connection getConnection() {
      return connection;
    }
    public Session getSession() {
      return session;
    }
    public Queue getQueue() {
      return queue;
    }
    public MessageConsumer getConsumer() {
      return consumer;
    }
    public MessageProducer getProducer() {
      return producer;
    }
  }

  public class CoreHandle {
    private final ServerLocator locator;
    private final ClientSessionFactory factory;
    private final ClientSession session;
    private ClientConsumer consumer = null;
    private ClientProducer producer = null;
    public CoreHandle(final ServerLocator locator, final ClientSessionFactory factory, final ClientSession session, ClientProducer producer, ClientConsumer consumer) {
      this.locator = locator;
      this.factory = factory;
      this.session = session;
      this.producer = producer;
      this.consumer = consumer;
    }
    public void close() throws ActiveMQException {
      if(producer != null)
        producer.close();
      if(consumer != null)
        consumer.close();
      session.close();
      factory.close();
      locator.close();
    }
    public ServerLocator getLocator() {
      return locator;
    }
    public ClientSessionFactory getFactory() {
      return factory;
    }
    public ClientSession getSession() {
      return session;
    }
    public ClientConsumer getConsumer() {
      return consumer;
    }
    public ClientProducer getProducer() {
      return producer;
    }
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

  public class CoreMessageHandlerStub implements MessageHandler {
    private final String name;
    
    private List<String> messages = new ArrayList<String>();
    private int messageCount = 0;

    private Connection connection; 
    private ClientSession session; 
    
    public CoreMessageHandlerStub(final String name) {
      this.name = name;
    }

    @Override
    public void onMessage(ClientMessage message) {
      messageCount++;
      
      if (log.isDebugEnabled()) {
        String body = null;
        try {
          body = message.getBodyBuffer().readString();
        } catch (Exception e) {
          log.error("Failed while reading core message body", e);
        }

        log.debug("Received core message on '{}': {}", name, body);
        messages.add(body);     
      }

      try {
        //message.acknowledge();
        message.individualAcknowledge();
      } catch (ActiveMQException e) {
        log.error("Unable to acknowledge core message", e);
      }
    }
    public List<String> getMessages() {
      return messages;
    }
    public String getMessagesAsString() {
      String messagesString = "";
      for(String msg : messages) {
        messagesString += msg + "\n";
      }
      return messagesString;
    }

    public ClientSession getSession() {
      return session;
    }
    public void setSession(ClientSession session) {
      this.session = session;
    }
    
    public int getMessageCount() {
      return messageCount;
    }
    public void setMessageCount(int messageCount) {
      this.messageCount = messageCount;
    }
  }

  public class JmsMessageListenerStub implements MessageListener {
    private final String name;
    
    private List<String> messages = new ArrayList<String>();
    private int messageCount = 0;

    private Connection connection; 
    
    public JmsMessageListenerStub(final String name) {
      this.name = name;
    }

    @Override
    public void onMessage(javax.jms.Message message) {
      messageCount++;
      
      if (log.isDebugEnabled()) {
        String body = null;
        try {
          body = message.getBody(String.class);
        } catch (JMSException e) {
          log.error("Failed while reading jms message body", e);
        }

        log.debug("Received JMS message on '{}': {}", name, body);
        messages.add(body);     
      }

      try {
        message.acknowledge();
      } catch (JMSException e) {
        log.error("Unable to acknowledge jms message", e);
      }
    }
    public List<String> getMessages() {
      return messages;
    }
    public String getMessagesAsString() {
      String messagesString = "";
      for(String msg : messages) {
        messagesString += msg + "\n";
      }
      return messagesString;
    }

    public Connection getConnection() {
      return connection;
    }
    public void setConnection(Connection connection) {
      this.connection = connection;
    }

    public int getMessageCount() {
      return messageCount;
    }
    public void setMessageCount(int messageCount) {
      this.messageCount = messageCount;
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
    private CommandReceiver commandReceiver;
    
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
    public CommandReceiver getCommandReceiver() {
      return commandReceiver;
    }
    public void setCommandReceiver(CommandReceiver commandReceiver) {
      this.commandReceiver = commandReceiver;
    }
  }
}
