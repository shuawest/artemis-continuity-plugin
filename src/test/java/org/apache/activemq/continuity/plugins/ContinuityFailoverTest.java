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
package org.apache.activemq.continuity.plugins;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.core.ContinuityCommand;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityFailoverTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(ContinuityFailoverTest.class);
  
  @Test
  public void coreSwapoverTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-with-plugin.xml", "site1", "myuser", "mypass");
    ServerContext serverCtx2 = createServerContext("broker2-with-plugin.xml", "site2", "myuser", "mypass");
    serverCtx1.getServer().start();
    serverCtx2.getServer().start();
    Thread.sleep(2000L);

    log.debug("\n\nProducing test messages on broker1\n\n");
    produceMessages("vm://1", "myuser", "mypass", "example1", "test message", 100);
    
    log.debug("\n\nStarting consumer for test message on broker2\n\n");
    CoreMessageHandlerStub handler2 = new CoreMessageHandlerStub("broker2");
    ClientSession session2 = startCoreConsumer("vm://2", "myuser", "mypass", "example1-durable", handler2);
    
    log.debug("\n\nStarting consumer for test message on broker1\n\n");
    CoreMessageHandlerStub handler1 = new CoreMessageHandlerStub("broker1");
    ClientSession session1 = startCoreConsumer("vm://1", "myuser", "mypass", "example1-durable", handler1);

    Thread.sleep(20L);
    log.debug("\n\nKilling session on broker 1\n\n");
    session1.close();

    log.debug("\n\nKilled session on broker 1\n\n");
    
    log.debug("\n\nActivating broker 2 site\n\n");
    activateSite("vm://2");

    Thread.sleep(2000L);

    serverCtx1.getServer().start();
    Thread.sleep(2000L);

    log.debug("\n\n{} Messages consumed off broker1:\n{}", handler1.getMessageCount(), handler1.getMessagesAsString());
    log.debug("\n\n{} Messages consumed off broker2:\n{}", handler2.getMessageCount(), handler2.getMessagesAsString());
    assertThat("All messages not consumed extactly once", handler1.getMessageCount() + handler2.getMessageCount(), equalTo(100));

    log.debug("\n\nShutting down\n\n");

    session1.close();
    session2.close();
    serverCtx1.getServer().asyncStop(()->{ log.debug("server1 stopped again"); });
    serverCtx2.getServer().asyncStop(()->{ log.debug("server2 stopped"); });
  }

  @Test
  public void coreFailoverTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-with-plugin.xml", "site1", "myuser", "mypass");
    ServerContext serverCtx2 = createServerContext("broker2-with-plugin.xml", "site2", "myuser", "mypass");
    serverCtx1.getServer().start();
    serverCtx2.getServer().start();
    Thread.sleep(2000L);

    log.debug("\n\nProducing test messages on broker1\n\n");
    produceMessages("vm://1", "myuser", "mypass", "example1", "test message", 100);

    log.debug("\n\nStarting consumer for test message on broker2\n\n");
    CoreMessageHandlerStub handler2 = new CoreMessageHandlerStub("broker2");
    ClientSession session2 = startCoreConsumer("vm://2", "myuser", "mypass", "example1-durable", handler2);
    
    log.debug("\n\nStarting consumer for test message on broker1\n\n");
    ServerLocator locator1 = ActiveMQClient.createServerLocator("vm://1");
    ClientSessionFactory factory1 = locator1.createSessionFactory();
    ClientSession session1 = factory1.createSession("myuser", "mypass", false, false, false, false, 1);
    ClientConsumer consumer1 = session1.createConsumer("example1-durable");
    session1.start();
    for(int i=0; i < 50; i++) {
      ClientMessage msg = consumer1.receive();
      msg.acknowledge();
      session1.commit();
    }

    Thread.sleep(20L);

    log.debug("\n\nStopping server1\n\n");
    serverCtx1.getServer().asyncStop(()->{ log.debug("\n\nserver1 stopped\n\n"); });

    activateSite("vm://2");

    Thread.sleep(2000L);
    
    log.debug("\n\n{} Messages consumed off broker2:\n{}", handler2.getMessageCount(), handler2.getMessagesAsString());
    log.debug("broker2 example1-durable subject count:  {}", serverCtx2.getServer().locateQueue(SimpleString.toSimpleString("example1-durable")).getDurableMessageCount());
    log.debug("broker2 example1-durable outflow mirror: {}", serverCtx2.getServer().locateQueue(SimpleString.toSimpleString("example1-durable.out.mirror")).getDurableMessageCount());
    log.debug("broker2 example1-durable outflow acks:   {}", serverCtx2.getServer().locateQueue(SimpleString.toSimpleString("example1-durable.out.acks")).getDurableMessageCount());
    
    assertThat("All messages not consumed", handler2.getMessageCount(), equalTo(50));

    log.debug("Shutting down");

    session1.close();
    session2.close();
    serverCtx2.getServer().asyncStop(()->{ log.debug("server2 stopped"); });
  }

  @Test
  public void jmsSwapoverTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-with-plugin.xml", "site1", "myuser", "mypass");
    ServerContext serverCtx2 = createServerContext("broker2-with-plugin.xml", "site2", "myuser", "mypass");
    serverCtx1.getServer().start();
    serverCtx2.getServer().start();
    Thread.sleep(2000L);

    log.debug("\n\nProducing test messages on broker1\n\n");
    produceJmsMessages("vm://1", "myuser", "mypass", "example1", "test message", 100);

    log.debug("\n\nStarting consumer for test message on broker2\n\n");
    JmsMessageListenerStub handler2 = new JmsMessageListenerStub("broker2");
    Connection conn2 = startJmsConsumer("vm://2", "myuser", "mypass", "example1-durable", handler2);
    
    log.debug("\n\nStarting consumer for test message on broker1\n\n");
    JmsMessageListenerStub handler1 = new JmsMessageListenerStub("broker2");
    Connection conn1 = startJmsConsumer("vm://1", "myuser", "mypass", "example1-durable", handler1);

    Thread.sleep(20L);

    log.debug("\n\nStopping server1\n\n");
    conn1.close();

    activateSite("vm://2");

    Thread.sleep(2000L);
    
    log.debug("\n\n{} Messages consumed off broker1:\n{}", handler1.getMessageCount(), handler1.getMessagesAsString());
    log.debug("\n\n{} Messages consumed off broker2:\n{}", handler2.getMessageCount(), handler2.getMessagesAsString());
    log.debug("broker2 example1-durable subject count:  {}", serverCtx2.getServer().locateQueue(SimpleString.toSimpleString("example1-durable")).getDurableMessageCount());
    log.debug("broker2 example1-durable outflow mirror: {}", serverCtx2.getServer().locateQueue(SimpleString.toSimpleString("example1-durable.out.mirror")).getDurableMessageCount());
    log.debug("broker2 example1-durable outflow acks:   {}", serverCtx2.getServer().locateQueue(SimpleString.toSimpleString("example1-durable.out.acks")).getDurableMessageCount());
    
    assertThat("All messages not consumed", handler1.getMessageCount() + handler2.getMessageCount(), greaterThanOrEqualTo(100));

    log.debug("Shutting down");

    conn1.close();
    conn2.close();
    serverCtx2.getServer().asyncStop(()->{ log.debug("server2 stopped"); });
  }

  @Test
  public void jmsTransactionalFailoverTest() throws Exception {
    ServerContext serverCtx1 = createServerContext("broker1-with-plugin.xml", "site1", "myuser", "mypass");
    ServerContext serverCtx2 = createServerContext("broker2-with-plugin.xml", "site2", "myuser", "mypass");
    serverCtx1.getServer().start();
    serverCtx2.getServer().start();
    Thread.sleep(2000L);

    log.debug("\n\nProducing test messages on broker1\n\n");
    produceJmsMessages("vm://1", "myuser", "mypass", "example1", "test message", 100);

    log.debug("\n\nStarting consumer for test message on broker2\n\n");
    JmsMessageListenerStub handler2 = new JmsMessageListenerStub("broker2");
    Connection conn2 = startJmsConsumer("vm://2", "myuser", "mypass", "example1-durable", handler2);
    
    ConnectionFactory factory1 = new ActiveMQConnectionFactory("tcp://localhost:61616");
    Connection conn1 = factory1.createConnection("myuser", "mypass");
    Session session1 = conn1.createSession(true, Session.AUTO_ACKNOWLEDGE);
    javax.jms.Queue queue1 = session1.createQueue("example1-durable");
    MessageConsumer consumer1 = session1.createConsumer(queue1);
    conn1.start();
    for(int i=0; i < 50; i++) {
      Message msg = consumer1.receive();
      session1.commit();
    }

    Thread.sleep(20L);

    log.debug("\n\nStopping server1\n\n");
    serverCtx1.getServer().asyncStop(()->{ log.debug("\n\nserver1 stopped\n\n"); });

    activateSite("vm://2");

    Thread.sleep(2000L);
    
    log.debug("\n\n{} Messages consumed off broker2:\n{}", handler2.getMessageCount(), handler2.getMessagesAsString());
    log.debug("broker2 example1-durable subject count:  {}", serverCtx2.getServer().locateQueue(SimpleString.toSimpleString("example1-durable")).getDurableMessageCount());
    log.debug("broker2 example1-durable outflow mirror: {}", serverCtx2.getServer().locateQueue(SimpleString.toSimpleString("example1-durable.out.mirror")).getDurableMessageCount());
    log.debug("broker2 example1-durable outflow acks:   {}", serverCtx2.getServer().locateQueue(SimpleString.toSimpleString("example1-durable.out.acks")).getDurableMessageCount());
    
    assertThat("All messages not consumed", handler2.getMessageCount(), greaterThan(0));

    log.debug("Shutting down");

    conn1.close();
    conn2.close();
    serverCtx2.getServer().asyncStop(()->{ log.debug("server2 stopped"); });
  }

  private void activateSite(String url) throws Exception {
    ContinuityCommand cmdActivate = new ContinuityCommand();
    cmdActivate.setAction(ContinuityCommand.ACTION_ACTIVATE_SITE);
    produceMessage("vm://2", "myuser", "mypass", "artemis.continuity.commands.in", ContinuityCommand.toJSON(cmdActivate));
  }

}
