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

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;

public class ContinuityException extends ActiveMQException {
  private static final long serialVersionUID = -1472905037395776112L;

  private final String message;
  private Exception innerException; 

  public ContinuityException(final String message) {
    super(message, ActiveMQExceptionType.INTERNAL_ERROR);
    this.message = message;
  }

  public ContinuityException(final String message, final Exception e) {
    super(message, e, ActiveMQExceptionType.INTERNAL_ERROR);
    this.message = message;
    this.innerException = e;
  }

  public String getMessage() {
    return message;
  }

  public Exception getInnerException() {
    return innerException;
  }
}
