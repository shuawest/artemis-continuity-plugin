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

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.core.server.ActiveMQScheduledComponent;
import org.apache.activemq.artemis.core.server.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InflowAcksConsumedWatcher extends ActiveMQScheduledComponent {

    private static final Logger log = LoggerFactory.getLogger(InflowAcksConsumedWatcher.class);

    private final ContinuityService continuityService;

    public InflowAcksConsumedWatcher(ContinuityService continuityService, long checkPeriod, ScheduledExecutorService scheduledExecutorService, Executor executor) {
      super(scheduledExecutorService, executor, checkPeriod, TimeUnit.MILLISECONDS, false);
      this.continuityService = continuityService;
    }

    @Override
    public void run() {
      if (log.isDebugEnabled()) {
        log.debug("Checking if inflow ack queues are consumed");
      }

      boolean allExhausted = true;
      List<Queue> inflowAckQueues = continuityService.getInfowAcksQueues();
      for(Queue q : inflowAckQueues) {
        long messageCount = q.getMessageCount();
        if (messageCount > 0) {
          allExhausted = false;
          
          if(log.isDebugEnabled()) {
            log.debug("Queue '{}' is not yet exhausted (count: {})", q.getName(), messageCount);
          }
        }
      }

      if (allExhausted) {
        if (log.isInfoEnabled()) {
          log.info("All inflow ack queues are consumed");
        }

        try {
          continuityService.handleInflowQueuesConsumed();
        } catch (ContinuityException e) {
          log.error("Unable to handle inflow queues consumed event");
        } finally {
          // stop this executor
          stop();
        }
      }
    }
  }