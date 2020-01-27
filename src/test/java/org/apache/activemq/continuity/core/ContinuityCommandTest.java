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

import org.apache.activemq.continuity.ContinuityTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuityCommandTest extends ContinuityTestBase {
 
  private static final Logger log = LoggerFactory.getLogger(ContinuityCommandTest.class);

  @Test
  public void toAndFromJson() throws Exception {
    ContinuityCommand cmd = new ContinuityCommand();
    cmd.setAction("didsomething");
    cmd.setAddress("myaddress");
    cmd.setQueue("myqueue");
    String cmdJson = ContinuityCommand.toJSON(cmd);
    ContinuityCommand actualCmd = ContinuityCommand.fromJSON(cmdJson);

    assertThat(actualCmd, notNullValue());
    assertThat(actualCmd.getAction(), equalTo(cmd.getAction()));
    assertThat(actualCmd.getAddress(), equalTo(cmd.getAddress()));
    assertThat(actualCmd.getQueue(), equalTo(cmd.getQueue()));
  }

}
