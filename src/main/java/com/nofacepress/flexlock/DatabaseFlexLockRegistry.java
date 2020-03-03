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

import java.sql.SQLException;
import com.nofacepress.flexlock.adapter.DatabaseFlexLockAdapter;

/**
 * M
 */
public class DatabaseFlexLockRegistry<KeyType> extends FlexLockRegistry<KeyType> {

  public DatabaseFlexLockRegistry(final String dbDriver, final String dbUrl, final String dbUser,
      final String dbPassword) throws ClassNotFoundException, SQLException {
    super(new DatabaseFlexLockAdapter<KeyType>(dbDriver, dbUrl, dbUser, dbPassword));
  }

  public DatabaseFlexLockRegistry(final String dbDriver, final String dbUrl, final String dbUser,
      final String dbPassword, final String tableName) throws ClassNotFoundException, SQLException {
    super(new DatabaseFlexLockAdapter<KeyType>(dbDriver, dbUrl, dbUser, dbPassword, tableName));
  }

  public DatabaseFlexLockRegistry(final String dbDriver, final String dbUrl, final String dbUser,
      final String dbPassword, final String tableName, final String primaryKeyName, final String expiresColumnName,
      final String ownerColumnName) throws ClassNotFoundException, SQLException {
    super(new DatabaseFlexLockAdapter<KeyType>(dbDriver, dbUrl, dbUser, dbPassword, tableName, primaryKeyName,
        expiresColumnName, ownerColumnName));
  }
}
