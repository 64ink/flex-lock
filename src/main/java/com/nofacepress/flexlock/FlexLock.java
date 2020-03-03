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
package com.nofacepress.flexlock;

import com.nofacepress.flexlock.exception.FlexLockException;
import com.nofacepress.flexlock.handle.FlexLockHandle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@RequiredArgsConstructor
@ToString
public class FlexLock<KeyType> {
  @Getter
  private final KeyType key;
  @Getter
  private final FlexLockRegistry<KeyType> registry;
  @Getter
  @Setter
  private FlexLockHandle handle;

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof FlexLock)
      return key.equals(FlexLock.class.cast(obj).key);
    if (obj instanceof String)
      return key.equals(obj);
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  void lock(final int maxTimeInMilliseconds) throws InterruptedException, FlexLockException {
    handle = registry.lock(key, maxTimeInMilliseconds);
  }

  boolean tryLock(final int maxTimeInMilliseconds) throws FlexLockException {
    handle = registry.tryLock(key, maxTimeInMilliseconds);
    return handle != null;
  }

  void unlock() throws FlexLockException {
    registry.unlock(handle);
  }

}
