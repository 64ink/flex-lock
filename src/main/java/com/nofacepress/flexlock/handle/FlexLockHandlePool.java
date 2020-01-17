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

@ToString
public class FlexLockHandlePool<T> {

  private Object UNUSED = new Object();
  private final Queue<FlexLockHandle> availableHandles = new ArrayDeque<FlexLockHandle>();
  private final List<Object> dataStorage = new ArrayList<Object>();

  @SuppressWarnings("unchecked")
  public synchronized T release(FlexLockHandle handle) {
    Object data = dataStorage.get(handle.getIndex());
    if (data == UNUSED) {
      return null;
    }
    dataStorage.set(handle.getIndex(), UNUSED);
    availableHandles.add(handle);
    return (T) data;
  }

  public synchronized FlexLockHandle reserve(T data) {
    FlexLockHandle next = availableHandles.poll();
    if (next == null) {
      dataStorage.add(data);
      return new FlexLockHandle(dataStorage.size() - 1);
    }

    dataStorage.set(next.getIndex(), data);
    return next;
  }

}
