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
package com.nofacepress.flexlock.handle;

import java.util.UUID;
import lombok.Data;

/**
 * Handle representing the instance of a locked FlexLock. This is intended to be
 * accessed via the FlexLockHandlePool.
 */
@Data
public class FlexLockHandle {

  private final Integer index;
  private final String uuid = UUID.randomUUID().toString();

  public FlexLockHandle() {
    this.index = 0;
  }

  /**
   * Constructor
   * 
   * @param index the index in the FlexLockHandlePool.
   */
  FlexLockHandle(final Integer index) {
    this.index = index;
  }
}
