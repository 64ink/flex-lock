/*
 * Copyright 2018,2020 No Face Press, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.nofacepress.flexlock.exception;

public class AlreadyLockedException extends FlexLockException {

  private static final long serialVersionUID = -6441687340338559460L;

  public AlreadyLockedException() {}

  public AlreadyLockedException(String message) {
    super(message);
  }

  public AlreadyLockedException(String message, Throwable cause) {
    super(message, cause);
  }

  public AlreadyLockedException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public AlreadyLockedException(Throwable cause) {
    super(cause);
  }

}
