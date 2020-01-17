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

import java.util.HashMap;
import java.util.Map;
import com.nofacepress.flexlock.adapter.FlexLockAdapter;
import com.nofacepress.flexlock.exception.AlreadyLockedException;
import com.nofacepress.flexlock.exception.FlexLockException;
import com.nofacepress.flexlock.handle.FlexLockHandle;
import com.nofacepress.flexlock.handle.FlexLockHandlePool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class FlexLockRegistry {

  private static class Mutex {
    final String key;
    long timeout = 0;
    FlexLockHandle handle = null;
    int waiters = 0;

    Mutex(String key) {
      this.key = key;
    }
  }

  private final FlexLockHandlePool<Mutex> handles = new FlexLockHandlePool<Mutex>();
  private final Map<String, Mutex> locks = new HashMap<String, Mutex>();
  @Getter
  @Setter
  private FlexLockAdapter adapter = null;
  @Getter
  @Setter
  private long pollingIntervalInMilliseconds = 100;

  public FlexLockRegistry(FlexLockAdapter adapter) {
    this.adapter = adapter;
  }

  public void forceUnlock(String key) throws FlexLockException {
    final Mutex mutex = getMutex(key);
    if (mutex == null)
      return;
    synchronized (mutex) {
      FlexLockException err = null;
      if (mutex.handle == null)
        return;
      if (adapter != null) {
        try {
          adapter.forceUnlock(mutex.key);
        } catch (Exception e) {
          err = new FlexLockException(e);
        }
      }
      mutex.timeout = 0;
      mutex.handle = null;
      if (mutex.waiters > 0) {
        mutex.notify();
      }
      if (err != null)
        throw err;
    }
  }

  private synchronized Mutex getMutex(String key) throws FlexLockException {
    Mutex mutex = locks.get(key);
    if (mutex == null) {
      if (adapter != null) {
        try {
          adapter.verifyKey(key);
        } catch (Exception e) {
          throw new FlexLockException(e);
        }
      }
      mutex = new Mutex(key);
      locks.put(key, mutex);
    }
    return mutex;
  }

  public FlexLockHandle lock(String key, int maxTimeInMilliseconds)
      throws InterruptedException, FlexLockException {
    final Mutex mutex = getMutex(key);
    for (;;) {
      synchronized (mutex) {
        try {
          return lockWhileSynchronized(mutex, maxTimeInMilliseconds);
        } catch (AlreadyLockedException ignoreThisException) {
        }

        mutex.waiters++;
        mutex.wait(Math.max(
            Math.min(pollingIntervalInMilliseconds, mutex.timeout - System.currentTimeMillis() + 1),
            1));
        mutex.waiters--;
      }
    }
  }

  private FlexLockHandle lockWhileSynchronized(Mutex mutex, int maxTimeInMilliseconds)
      throws FlexLockException {

    long now = System.currentTimeMillis();
    if (mutex.handle != null && mutex.timeout >= now) {
      throw new AlreadyLockedException();
    }

    long expireTime = now + maxTimeInMilliseconds;
    FlexLockHandle handle = handles.reserve(mutex);

    if (adapter == null) {
      mutex.timeout = expireTime;
      mutex.handle = handle;
      return handle;
    }

    FlexLockException err = null;
    try {
      if (adapter.tryLock(mutex.key, handle, now, expireTime)) {
        mutex.timeout = expireTime;
        mutex.handle = handle;
        return handle;
      }
    } catch (FlexLockException e) {
      err = e;
    } catch (Exception e) {
      err = new FlexLockException(e);
    }

    handles.release(handle);
    throw err == null ? new AlreadyLockedException() : err;
  }

  public FlexLockHandle tryLock(String key, int maxTimeInMilliseconds)
      throws AlreadyLockedException, FlexLockException {
    final Mutex mutex = getMutex(key);
    synchronized (mutex) {
      return lockWhileSynchronized(mutex, maxTimeInMilliseconds);
    }
  }

  public void unlock(FlexLockHandle handle) throws FlexLockException {
    if (handle == null)
      return;
    final Mutex mutex = handles.release(handle);
    if (mutex == null)
      return;
    synchronized (mutex) {
      FlexLockException err = null;
      if (mutex.handle != handle)
        return;
      if (adapter != null) {
        try {
          adapter.unlock(mutex.key, mutex.handle);
        } catch (Exception e) {
          err = new FlexLockException(e);
        }
      }
      mutex.timeout = 0;
      mutex.handle = null;
      if (mutex.waiters > 0) {
        mutex.notify();
      }
      if (err != null)
        throw err;
    }
  }

}
