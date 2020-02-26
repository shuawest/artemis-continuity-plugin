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

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.core.server.ActiveMQScheduledComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ActivationTimeoutExecutor extends ActiveMQScheduledComponent {

    private static final Logger log = LoggerFactory.getLogger(ActivationTimeoutExecutor.class);

    private final ContinuityService continuityService;
    private final long duration;

    public ActivationTimeoutExecutor(ContinuityService continuityService, long duration, ScheduledExecutorService scheduledExecutorService, Executor executor) {
      super(scheduledExecutorService, executor, duration, TimeUnit.MILLISECONDS, false);
      this.continuityService = continuityService;
      this.duration = duration; 
    }

    @Override
    public void run() {
      if (log.isInfoEnabled()) {
        log.info("Site activation timeout reached ({})", duration);
      }

      try {
        continuityService.startSubjectQueueDelivery();
      } catch(ContinuityException e) {
        log.error("Unable to start subject queue delivery after activation timeout", e);
      }

      stop();
    }
  }