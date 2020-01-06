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
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicateIdPluginTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(DuplicateIdPluginTest.class);

  @Test
  public void mockedMessageTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
 
    String addressName = "async-sample1";

    Message msgMock = mock(Message.class);
    when(msgMock.getAddress()).thenReturn(addressName);
    when(msgMock.getDuplicateIDBytes()).thenReturn(null);
    when(continuityCtx.getService().isSubjectAddress(addressName)).thenReturn(true);
    
    DuplicateIdPlugin plugin = new DuplicateIdPlugin(continuityCtx.getService());
    
    plugin.beforeSend(null, null, msgMock, true, true);

    ArgumentCaptor<SimpleString> keyCaptor = ArgumentCaptor.forClass(SimpleString.class); 
    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class); 
    verify(msgMock, times(1)).putStringProperty(keyCaptor.capture(), valueCaptor.capture());
    assertThat("duplicate id header was wrong", keyCaptor.getValue(), equalTo(Message.HDR_DUPLICATE_DETECTION_ID));
    assertThat("duplicate id value was null", keyCaptor.getValue(), notNullValue());

    verify(msgMock, times(1).description("reencode was not called on the message")).reencode();
  }

  @Test
  public void mockedMessageNonTargetAddressTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
 
    String addressName = "non-targetAddress";

    Message msgMock = mock(Message.class);
    when(msgMock.getAddress()).thenReturn(addressName);
    when(msgMock.getDuplicateIDBytes()).thenReturn(null);
    when(continuityCtx.getService().isSubjectAddress(addressName)).thenReturn(false);
    
    DuplicateIdPlugin plugin = new DuplicateIdPlugin(continuityCtx.getService());
    
    plugin.beforeSend(null, null, msgMock, true, true);

    verify(msgMock, times(0)).getDuplicateIDBytes();
    verify(msgMock, times(0)).putStringProperty(any(SimpleString.class), any(String.class));
    verify(msgMock, times(0)).reencode();
  }

  @Test
  public void mockedMessageAlreadyHasDuplicateIdTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
 
    String addressName = "async-sample1";

    Message msgMock = mock(Message.class);
    when(msgMock.getAddress()).thenReturn(addressName);
    when(msgMock.getDuplicateIDBytes()).thenReturn("asdf-asdf-asdf".getBytes());
    when(continuityCtx.getService().isSubjectAddress(addressName)).thenReturn(true);
    
    DuplicateIdPlugin plugin = new DuplicateIdPlugin(continuityCtx.getService());
    
    plugin.beforeSend(null, null, msgMock, true, true);

    verify(msgMock, times(1)).getDuplicateIDBytes();
    verify(msgMock, times(0)).putStringProperty(any(SimpleString.class), any(String.class));
    verify(msgMock, times(0)).reencode();
  }

  @Test
  public void actualMessageTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    MessageHandler handlerMock = mock(MessageHandler.class);
    
    String addressName = "async-sample1";
    String queueName = "async-sample1";
    String expectedMessage = "test message";

    when(continuityCtx.getService().isSubjectAddress(addressName)).thenReturn(true);
    
    DuplicateIdPlugin plugin = new DuplicateIdPlugin(continuityCtx.getService());
    serverCtx.getServer().getConfiguration().registerBrokerPlugin(plugin);
    
    produceAndConsumeMessage(continuityCtx, serverCtx, addressName, queueName, handlerMock, expectedMessage, null);
    
    ArgumentCaptor<ClientMessage> msgCaptor = ArgumentCaptor.forClass(ClientMessage.class);
    verify(handlerMock, times(1)).onMessage(msgCaptor.capture());
    ClientMessage receivedMessage = msgCaptor.getValue();
    log.debug("Received message: {}", receivedMessage);

    assertThat("message was not received", receivedMessage.getBodyBuffer().readString(), equalTo(expectedMessage));
    assertThat("message duplicate id header was null", receivedMessage.getStringProperty(Message.HDR_DUPLICATE_DETECTION_ID), notNullValue());
    assertThat("message duplicate id bytes was null", receivedMessage.getDuplicateIDBytes(), notNullValue());
  }

  @Test
  public void actualMessageAlreadyHasDuplicateIdTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    MessageHandler handlerMock = mock(MessageHandler.class);
    
    String addressName = "async-sample1";
    String queueName = "async-sample1";
    String expectedMessage = "test message";
    String expectedUuid = UUID.randomUUID().toString();

    when(continuityCtx.getService().isSubjectAddress(addressName)).thenReturn(true);
    
    DuplicateIdPlugin plugin = new DuplicateIdPlugin(continuityCtx.getService());
    serverCtx.getServer().getConfiguration().registerBrokerPlugin(plugin);
    
    produceAndConsumeMessage(continuityCtx, serverCtx, addressName, queueName, handlerMock, expectedMessage, expectedUuid);
    
    ArgumentCaptor<ClientMessage> msgCaptor = ArgumentCaptor.forClass(ClientMessage.class);
    verify(handlerMock, times(1)).onMessage(msgCaptor.capture());
    ClientMessage receivedMessage = msgCaptor.getValue();
    log.debug("Received message: {}", receivedMessage);

    assertThat("message was not received", receivedMessage.getBodyBuffer().readString(), equalTo(expectedMessage));
    assertThat("message duplicate id header was null", receivedMessage.getStringProperty(Message.HDR_DUPLICATE_DETECTION_ID), equalTo(expectedUuid));
    assertThat("message duplicate id bytes was null", receivedMessage.getDuplicateIDBytes(), notNullValue());
  }
  
}
