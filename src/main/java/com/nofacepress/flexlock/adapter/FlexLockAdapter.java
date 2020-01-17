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
package com.nofacepress.flexlock.adapter;

import com.nofacepress.flexlock.handle.FlexLockHandle;

/**
 * Internal interface for handling lock activity from different sources.
 */
public interface FlexLockAdapter {

  /**
   * Ensure that the key exists, creating a new one if necessary.
   * 
   * @param key the key identifying the lock
   * @throws Exception an unexpected error
   */
  void ensureKeyExistsCreatingIfNessessary(String key) throws Exception;

  /**
   * Unlocks a FlexLock even if the caller is not the current owner of the lock.
   * 
   * @param key the key identifying the lock
   * @throws Exception any exception for error
   */
  void forceUnlock(String key) throws Exception;

  /**
   * Tries to obtain a lock without blocking.
   * 
   * @param key the key identifying the lock
   * @param handle the associated handle.
   * @param now the current time
   * @param expireTime the expiration time for the lock
   * @return true if successful
   * @throws Exception an unexpected error
   */
  boolean tryLock(String key, FlexLockHandle handle, long now, long expireTime) throws Exception;

  /**
   * Unlocks a lock. An non-existing lock or already expired lock is ignored.
   * 
   * @param key the key identifying the lock
   * @param handle the associated handle.
   * @throws Exception an unexpected error
   */
  void unlock(String key, FlexLockHandle handle) throws Exception;

}
