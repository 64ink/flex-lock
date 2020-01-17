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

public interface FlexLockAdapter {

  void forceUnlock(String key) throws Exception;

  boolean tryLock(String key, FlexLockHandle handle, long now, long expireTime) throws Exception;

  void unlock(String key, FlexLockHandle handle) throws Exception;

  void verifyKey(String key) throws Exception;

}
