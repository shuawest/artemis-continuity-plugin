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

public final class OutflowExhaustedWatcher extends ActiveMQScheduledComponent {

    private static final Logger log = LoggerFactory.getLogger(OutflowExhaustedWatcher.class);

    private final ContinuityService continuityService;

    public OutflowExhaustedWatcher(ContinuityService continuityService, long checkPeriod, ScheduledExecutorService scheduledExecutorService,
        Executor executor) {
      super(scheduledExecutorService, executor, checkPeriod, TimeUnit.MILLISECONDS, false);
      this.continuityService = continuityService;
    }

    @Override
    public void run() {
      if (log.isDebugEnabled()) {
        log.debug("Checking if outlfow is exhausted");
      }

      boolean allExhausted = true;
      List<Queue> outflowQueues = continuityService.getOutflowQueues();
      for(Queue q : outflowQueues) {
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
          log.info("All outflow is exhausted");
        }

        try {
          continuityService.handleOutflowExhausted();
        } catch (ContinuityException e) {
          log.error("Unable to notify that outlfow is exhausted");
        } finally {
          // stop this executor
          stop();
        }
      }
    }
  }