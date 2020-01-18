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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import lombok.ToString;

/**
 * Map of handles and objects with a method to reserve access to a single object
 * in the list. This is needed over native locking because the access required
 * is beyond call scope.
 *
 * @param <T> the lock implementation class.
 */
@ToString
public class FlexLockHandlePool<T> {

  private final Object UNUSED = new Object();
  private final Queue<FlexLockHandle> availableHandles = new ArrayDeque<FlexLockHandle>();
  private final List<Object> dataStorage = new ArrayList<Object>();

  /**
   * Releases the handle from exclusive access.
   * 
   * @param handle the handle
   * @return the data held by the handle
   */
  @SuppressWarnings("unchecked")
  public synchronized T release(final FlexLockHandle handle) {
    final Object data = dataStorage.get(handle.getIndex());
    if (data == UNUSED) {
      return null;
    }
    dataStorage.set(handle.getIndex(), UNUSED);
    availableHandles.add(handle);
    return (T) data;
  }

  /**
   * Reserves access to the data.
   * 
   * @param data the data be reserved.
   * @return handle to the data, needed to release the reservation.
   */
  public synchronized FlexLockHandle reserve(final T data) {
    final FlexLockHandle next = availableHandles.poll();
    if (next == null) {
      dataStorage.add(data);
      return new FlexLockHandle(dataStorage.size() - 1);
    }

    dataStorage.set(next.getIndex(), data);
    return next;
  }

}
