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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.continuity.ContinuityTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityServiceTest extends ContinuityTestBase {

  private static final Logger log = LoggerFactory.getLogger(ContinuityServiceTest.class);

  @Test
  public void constructorTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    when(continuityCtx.getConfig().getAddresses()).thenReturn(Arrays.asList("example1","example2"));
    when(continuityCtx.getConfig().getOutflowMirrorSuffix()).thenReturn(".out.mirror");
    when(continuityCtx.getConfig().getOutflowAcksSuffix()).thenReturn(".out.acks");
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getConfig().getInflowAcksSuffix()).thenReturn(".in.acks");

    ContinuityService svc = new ContinuityService(serverCtx.getServer(), continuityCtx.getConfig());

    assertThat("ContinuitySevice server", svc.getServer(), notNullValue());
    assertThat("ContinuitySevice config", svc.getConfig(), notNullValue());

    assertThat("ContinuitySevice isSubjectAddress", svc.isSubjectAddress("example1"), equalTo(true));
    assertThat("ContinuitySevice isSubjectAddress", svc.isSubjectAddress("example2"), equalTo(true));
    assertThat("ContinuitySevice isSubjectAddress", svc.isSubjectAddress("example2.out.mirror"), equalTo(false));
    assertThat("ContinuitySevice isSubjectAddress", svc.isSubjectAddress("asdf"), equalTo(false));

    assertThat("ContinuitySevice isSubjectQueue", svc.isSubjectQueue("example1-durable"), equalTo(true));
    assertThat("ContinuitySevice isSubjectQueue", svc.isSubjectQueue("example1-nondurable"), equalTo(false));
    assertThat("ContinuitySevice isSubjectQueue", svc.isSubjectQueue("asdf"), equalTo(false));

    assertThat("ContinuitySevice isOutflowMirrorAddress", svc.isOutflowMirrorAddress("example1.out.mirror"), equalTo(true));
    assertThat("ContinuitySevice isOutflowMirrorAddress", svc.isOutflowMirrorAddress("example1.out.mirror.asdf"), equalTo(false));

    assertThat("ContinuitySevice isOutflowAcksAddress", svc.isOutflowAcksAddress("example1.out.acks"), equalTo(true));
    assertThat("ContinuitySevice isOutflowAcksAddress", svc.isOutflowAcksAddress("example1.out.acks.asdf"), equalTo(false));

    assertThat("ContinuitySevice isInflowMirrorAddress", svc.isInflowMirrorAddress("example1.in.mirror"), equalTo(true));
    assertThat("ContinuitySevice isInflowMirrorAddress", svc.isInflowMirrorAddress("example1.in.mirror.asdf"), equalTo(false));

    assertThat("ContinuitySevice isInflowAcksAddress", svc.isInflowAcksAddress("example1.in.acks"), equalTo(true));
    assertThat("ContinuitySevice isInflowAcksAddress", svc.isInflowAcksAddress("example1.in.acks.asdf"), equalTo(false));
    
    CommandManager cmdMgrMock = mock(CommandManager.class);
    svc.registerCommandManager(cmdMgrMock);
    assertThat(svc.getCommandManager(), equalTo(cmdMgrMock));

    ContinuityFlow flowMock = mock(ContinuityFlow.class);
    svc.registerContinuityFlow("example1", flowMock);
    assertThat(svc.locateFlow("example1"), equalTo(flowMock));
    assertThat(svc.locateFlow("asdf"), nullValue());
  }

  @Test
  public void initializeTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    when(continuityCtx.getConfig().getAddresses()).thenReturn(Arrays.asList("async-sample1"));
    when(continuityCtx.getConfig().getOutflowMirrorSuffix()).thenReturn(".out.mirror");
    when(continuityCtx.getConfig().getOutflowAcksSuffix()).thenReturn(".out.acks");
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getConfig().getInflowAcksSuffix()).thenReturn(".in.acks");

    ContinuityService svc = new ContinuityService(serverCtx.getServer(), continuityCtx.getConfig());
    svc.initialize();

    assertThat(svc.getCommandManager(), notNullValue());
  }

  @Test
  public void handleAddQueueTest() throws Exception {
    ServerContext serverCtx = createServerContext("broker1-noplugin.xml", "primary-server", "myuser", "mypass");
    ContinuityContext continuityCtx = createMockContext(serverCtx, "primary", 1);
    serverCtx.getServer().start();

    when(continuityCtx.getConfig().getAddresses()).thenReturn(Arrays.asList("async-sample1"));
    when(continuityCtx.getConfig().getOutflowMirrorSuffix()).thenReturn(".out.mirror");
    when(continuityCtx.getConfig().getOutflowAcksSuffix()).thenReturn(".out.acks");
    when(continuityCtx.getConfig().getInflowMirrorSuffix()).thenReturn(".in.mirror");
    when(continuityCtx.getConfig().getInflowAcksSuffix()).thenReturn(".in.acks");

    ContinuityService svc = new ContinuityService(serverCtx.getServer(), continuityCtx.getConfig());

    Queue queue = serverCtx.getServer().locateQueue(SimpleString.toSimpleString("async-sample1"));

    svc.handleAddQueue(queue);

    assertThat(svc.locateFlow("async-sample1"), notNullValue());
  }

}
