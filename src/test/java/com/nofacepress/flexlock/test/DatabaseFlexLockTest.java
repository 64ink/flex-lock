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
package com.nofacepress.flexlock.test;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.nofacepress.flexlock.DatabaseFlexLockRegistry;
import com.nofacepress.flexlock.FlexLockRegistry;
import com.nofacepress.flexlock.exception.FlexLockException;
import com.nofacepress.flexlock.handle.FlexLockHandle;

@RunWith(Run100.class)
public class DatabaseFlexLockTest {

  public static final String DB_TABLE_NAME = "TESTMUTEX";
  public static final String DB_DRIVER = "org.h2.Driver";
  public static final String DB_USER = "";
  public static final String DB_PASSWORD = "";
  public static final String DB_URL =
      "jdbc:h2:mem:test;INIT=RUNSCRIPT FROM 'classpath:initTestData.sql'";

  @Test
  public void testLockAndUnlock()
      throws InterruptedException, FlexLockException, ClassNotFoundException, SQLException {
    final FlexLockRegistry registry = new DatabaseFlexLockRegistry(DB_DRIVER, DB_URL, DB_USER, DB_PASSWORD,
        DB_TABLE_NAME);
    FlexLockHandle handle = registry.lock("key", 1000);
    registry.unlock(handle);
    handle = registry.lock("key", 1000);
    registry.unlock(handle);
  }

  @Test
  public void testLockExpires() throws InterruptedException, FlexLockException, ClassNotFoundException, SQLException {
    final FlexLockRegistry registry = new DatabaseFlexLockRegistry(DB_DRIVER, DB_URL, DB_USER, DB_PASSWORD,
        DB_TABLE_NAME);
    final long start = System.currentTimeMillis();
    final FlexLockHandle handle = registry.lock("key", 500);
    final FlexLockHandle handle2 = registry.lock("key", 1000);
    final long stop = System.currentTimeMillis();
    registry.unlock(handle);
    registry.unlock(handle2);
    final long diff = stop - start;
    System.out.printf("stop=%d start=%d diff=%d\n", start, stop, diff);
    assertTrue(diff > 400);
    assertTrue(diff < 600);
  }

  @Test
  public void testUnlockTwice() throws InterruptedException, FlexLockException, ClassNotFoundException, SQLException {
    final FlexLockRegistry registry = new DatabaseFlexLockRegistry(DB_DRIVER, DB_URL, DB_USER, DB_PASSWORD,
        DB_TABLE_NAME);
    final FlexLockHandle handle = registry.lock("key", 1000);
    registry.unlock(handle);
    registry.unlock(handle);
  }

  @Test
  public void testUnlockNull() throws InterruptedException, FlexLockException, ClassNotFoundException, SQLException {
    final FlexLockRegistry registry =
        new DatabaseFlexLockRegistry(DB_DRIVER, DB_URL, DB_USER, DB_PASSWORD, DB_TABLE_NAME);
    registry.unlock(null);
  }

}
