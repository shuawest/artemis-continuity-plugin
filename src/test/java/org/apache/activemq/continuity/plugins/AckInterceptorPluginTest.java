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

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.impl.AckReason;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.apache.activemq.continuity.core.AckInterceptor;
import org.apache.activemq.continuity.core.ContinuityFlow;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckInterceptorPluginTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(AckInterceptorPluginTest.class);

  @Test
  public void mockedMessageTest() throws Exception { 
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "AckInterceptorPluginTest.mockedMessageTest", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
 
    AckInterceptor interceptorMock = mock(AckInterceptor.class);
    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    Queue queueMock = mock(Queue.class);
    MessageReference refMock = mock(MessageReference.class);
    Message msgMock = mock(Message.class);
    AckReason reasonMock = mock(AckReason.class);

    String expectedQueueName = "async-sample1";
    String expectedUuid = UUID.randomUUID().toString();

    when(flowMock.getAckInterceptor()).thenReturn(interceptorMock);
    when(continuityCtx.getService().locateFlow(expectedQueueName)).thenReturn(flowMock);
    when(continuityCtx.getService().isSubjectQueue(any(Queue.class))).thenReturn(true);
    when(queueMock.isDurable()).thenReturn(true);
    when(queueMock.isTemporary()).thenReturn(false);
    when(queueMock.getName()).thenReturn(SimpleString.toSimpleString(expectedQueueName));
    when(refMock.getQueue()).thenReturn(queueMock);
    when(refMock.getMessage()).thenReturn(msgMock);
    when(msgMock.getStringProperty(Message.HDR_DUPLICATE_DETECTION_ID)).thenReturn(expectedUuid);

    AckInterceptorPlugin plugin = new AckInterceptorPlugin(continuityCtx.getService());
    plugin.messageAcknowledged(refMock, reasonMock, null);

    ArgumentCaptor<MessageReference> refCaptor = ArgumentCaptor.forClass(MessageReference.class);
    ArgumentCaptor<AckReason> reasonCaptor = ArgumentCaptor.forClass(AckReason.class);
    verify(interceptorMock, times(1)).handleMessageAcknowledgement(refCaptor.capture(), reasonCaptor.capture());
    MessageReference actualRef = refCaptor.getValue();
    AckReason actualReason = reasonCaptor.getValue();
    
    assertThat("msgRef was null", actualRef, notNullValue());
    assertThat("msgRef was null", actualReason, notNullValue());
  }

}
