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

/**
 * Main interface for obtaining and managing FlexLock's.
 */
@NoArgsConstructor
public class FlexLockRegistry<KeyType> {

  public static long DEFAULT_POLLING_INTERVAL_IN_MILLISECONDS = 100;

  private static class Mutex<KeyType> {
    final KeyType key;
    long timeout = 0;
    FlexLockHandle handle = null;
    int waiters = 0;

    Mutex(final KeyType key) {
      this.key = key;
    }
  }

  private final FlexLockHandlePool<Mutex<KeyType>> handles = new FlexLockHandlePool<Mutex<KeyType>>();
  private final Map<KeyType, Mutex<KeyType>> locks = new HashMap<KeyType, Mutex<KeyType>>();

  @Getter
  @Setter
  private FlexLockAdapter<KeyType> adapter = null;

  @Getter
  @Setter
  private long pollingIntervalInMilliseconds = DEFAULT_POLLING_INTERVAL_IN_MILLISECONDS;

  /**
   * Constructor
   * 
   * @param adapter the adapter to use for creating new FlexLock's.
   */
  public FlexLockRegistry(final FlexLockAdapter<KeyType> adapter) {
    this.adapter = adapter;
  }

  /**
   * Unlocks a FlexLock even if the caller is not the current owner of the lock.
   * 
   * @param key the key identifying the lock
   * @throws FlexLockException unexpected adapter exception
   */
  public void forceUnlock(final KeyType key) throws FlexLockException {
    final Mutex<KeyType> mutex = getMutex(key);
    if (mutex == null)
      return;
    synchronized (mutex) {
      FlexLockException err = null;
      if (mutex.handle == null)
        return;
      if (adapter != null) {
        try {
          adapter.forceUnlock(mutex.key);
        } catch (final Exception e) {
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

  /**
   * Returns a mutex from the registry, creating one if needed.
   * 
   * @param key the key identifying the lock
   * @throws FlexLockException unexpected exception
   * @return a new or existing mutex
   */
  private synchronized Mutex<KeyType> getMutex(final KeyType key) throws FlexLockException {
    Mutex<KeyType> mutex = locks.get(key);
    if (mutex == null) {
      if (adapter != null) {
        try {
          adapter.ensureKeyExistsCreatingIfNessessary(key);
        } catch (final Exception e) {
          throw new FlexLockException(e);
        }
      }
      mutex = new Mutex<KeyType>(key);
      locks.put(key, mutex);
    }
    return mutex;
  }

  /**
   * Locks a FlexLock. This will block until lock is obtained.
   * 
   * @param key                   the key identifying the lock
   * @param maxTimeInMilliseconds the maximum time to hold the lock. This is only
   *                              applied if it does not get unlocked in time.
   * @return A handle to the FlexLock
   * @throws InterruptedException if thread is interrupted
   * @throws FlexLockException    unexpected adapter exception
   */
  public FlexLockHandle lock(final KeyType key, final int maxTimeInMilliseconds)
      throws InterruptedException, FlexLockException {
    final Mutex<KeyType> mutex = getMutex(key);
    for (;;) {
      synchronized (mutex) {
        try {
          return lockWhileSynchronized(mutex, maxTimeInMilliseconds);
        } catch (final AlreadyLockedException ignoreThisException) {
        }

        mutex.waiters++;
        mutex
            .wait(Math.max(Math.min(pollingIntervalInMilliseconds, mutex.timeout - System.currentTimeMillis() + 1), 1));
        mutex.waiters--;
      }
    }
  }

  /**
   * Obtain the lock with the assumption that the thread is already synchronized.
   * 
   * @param mutex                 the mutex
   * @param maxTimeInMilliseconds the maximum time to hold the lock
   * @return the handle
   * @throws FlexLockException      unexpected adapter exception
   * @throws AlreadyLockedException if the FlexLock is already locked.
   */
  private FlexLockHandle lockWhileSynchronized(final Mutex<KeyType> mutex, final int maxTimeInMilliseconds)
      throws FlexLockException {

    final long now = System.currentTimeMillis();
    if (mutex.handle != null && mutex.timeout >= now) {
      throw new AlreadyLockedException();
    }

    final long expireTime = now + maxTimeInMilliseconds;
    final FlexLockHandle handle = handles.reserve(mutex);

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
    } catch (final FlexLockException e) {
      err = e;
    } catch (final Exception e) {
      err = new FlexLockException(e);
    }

    handles.release(handle);
    throw err == null ? new AlreadyLockedException() : err;
  }

  /**
   * Tries to obtain a lock without blocking.
   * 
   * @param key                   the key identifying the lock
   * @param maxTimeInMilliseconds the maximum time to hold the lock. This is only
   *                              applied if it does not get unlocked in time.
   * @return the handle
   * @throws FlexLockException      unexpected adapter exception
   * @throws AlreadyLockedException if the FlexLock is already locked.
   */
  public FlexLockHandle tryLock(final KeyType key, final int maxTimeInMilliseconds)
      throws AlreadyLockedException, FlexLockException {
    final Mutex<KeyType> mutex = getMutex(key);
    synchronized (mutex) {
      return lockWhileSynchronized(mutex, maxTimeInMilliseconds);
    }
  }

  /**
   * Unlocks a lock. An non-existing lock or already expired lock is ignored.
   * 
   * @param handle handle to lock.
   * @throws FlexLockException unexpected adapter exception.
   */
  public void unlock(final FlexLockHandle handle) throws FlexLockException {
    if (handle == null)
      return;
    final Mutex<KeyType> mutex = handles.release(handle);
    if (mutex == null)
      return;
    synchronized (mutex) {
      FlexLockException err = null;
      if (mutex.handle != handle)
        return;
      if (adapter != null) {
        try {
          adapter.unlock(mutex.key, mutex.handle);
        } catch (final Exception e) {
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
