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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.dbcp2.BasicDataSource;
import com.nofacepress.flexlock.handle.FlexLockHandle;
import lombok.ToString;

/**
 * Internal class for handling lock activity from a database.
 */
@ToString
public class DatabaseFlexLockAdapter implements FlexLockAdapter {

  private static class SQL {
    static final String TABLE_KEY = "[MUTEX_TABLE]";
    static final String TRY_LOCK_UPDATE =
        "update [MUTEX_TABLE] set owner=?, expire_time=? where mutex_id=? and expire_time<=?";
    static final String MUTEX_EXISTS = "select 1 from [MUTEX_TABLE] where mutex_id=?";
    static final String INSERT_MUTEX =
        "insert into [MUTEX_TABLE] (mutex_id, expire_time) values (?, 0)";
    static final String TRY_UNLOCK_UPDATE =
        "update [MUTEX_TABLE] set expire_time=0 where mutex_id=? and owner=?";
    static final String FORCE_UNLOCK_UPDATE =
        "update [MUTEX_TABLE] set expire_time=0 where mutex_id=?";
  }

  public static final String DEFAULT_TABLE_NAME = "virtual_mutexes";
  private static final int MAX_PREPARED_STATEMENTS = 20;

  private final BasicDataSource connectionPool;
  private final String tryLockStatementSql;
  private final String mutexExistsStatementSql;
  private final String insertMutexStatementSql;
  private final String tryUnlockStatementSql;
  private final String forceUnlockStatementSql;

  public DatabaseFlexLockAdapter(String dbDriver, String dbUrl, String dbUser, String dbPassword)
      throws SQLException, ClassNotFoundException {
    this(dbDriver, dbUrl, dbUser, dbPassword, DEFAULT_TABLE_NAME);
  }

  public DatabaseFlexLockAdapter(String dbDriver, String dbUrl, String dbUser, String dbPassword,
      String tableName) throws SQLException, ClassNotFoundException {

    tryLockStatementSql = SQL.TRY_LOCK_UPDATE.replace(SQL.TABLE_KEY, tableName);
    mutexExistsStatementSql = SQL.MUTEX_EXISTS.replace(SQL.TABLE_KEY, tableName);
    insertMutexStatementSql = SQL.INSERT_MUTEX.replace(SQL.TABLE_KEY, tableName);
    tryUnlockStatementSql = SQL.TRY_UNLOCK_UPDATE.replace(SQL.TABLE_KEY, tableName);
    forceUnlockStatementSql = SQL.FORCE_UNLOCK_UPDATE.replace(SQL.TABLE_KEY, tableName);

    connectionPool = new BasicDataSource();
    connectionPool.setDriverClassName(dbDriver);
    connectionPool.setUrl(dbUrl);
    connectionPool.setInitialSize(1);
    connectionPool.setPoolPreparedStatements(true);
    connectionPool.setMaxOpenPreparedStatements(MAX_PREPARED_STATEMENTS);
    if (dbUser != null && !dbUser.isEmpty()) {
      connectionPool.setUsername(dbUser);
      connectionPool.setPassword(dbPassword);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dtis.common.mutex.MutexAdapter#ensureKeyExistsCreatingIfNessessary(java.lang.String)
   */
  public void ensureKeyExistsCreatingIfNessessary(String key) throws Exception {
    PreparedStatement stmt = null;
    Connection connection = null;
    try {
      connection = connectionPool.getConnection();
      stmt = connection.prepareStatement(mutexExistsStatementSql);
      stmt.setString(1, key);
      ResultSet results = stmt.executeQuery();
      if (!results.next()) {
        // need to insert it
        stmt.close();
        stmt = null;
        stmt = connection.prepareStatement(insertMutexStatementSql);
        stmt.setString(1, key);
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      throw e;
    } finally {
      if (stmt != null)
        stmt.close();
      if (connection != null)
        connection.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dtis.common.mutex.MutexAdapter#forceUnlock(java.lang.String)
   */
  public void forceUnlock(String key) throws Exception {
    PreparedStatement stmt = null;
    Connection connection = null;
    try {
      connection = connectionPool.getConnection();
      stmt = connection.prepareStatement(forceUnlockStatementSql);
      stmt.setString(1, key);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw e;
    } finally {
      if (stmt != null)
        stmt.close();
      if (connection != null)
        connection.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dtis.common.mutex.MutexAdapter#tryLock(java.lang.String,
   * com.dtis.common.mutex.VirtualMutexHandle, long, long)
   */
  public boolean tryLock(String key, FlexLockHandle handle, long now, long expireTime)
      throws Exception {
    PreparedStatement stmt = null;
    Connection connection = null;
    try {
      connection = connectionPool.getConnection();
      stmt = connection.prepareStatement(tryLockStatementSql);
      stmt.setString(1, handle.getUuid());
      stmt.setLong(2, expireTime);
      stmt.setString(3, key);
      stmt.setLong(4, now);
      int updates = stmt.executeUpdate();
      return updates > 0;
    } catch (SQLException e) {
      throw e;
    } finally {
      if (stmt != null)
        stmt.close();
      if (connection != null)
        connection.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dtis.common.mutex.MutexAdapter#unlock(java.lang.String,
   * com.dtis.common.mutex.VirtualMutexHandle)
   */
  public void unlock(String key, FlexLockHandle handle) throws Exception {
    PreparedStatement stmt = null;
    Connection connection = null;
    try {
      connection = connectionPool.getConnection();
      stmt = connection.prepareStatement(tryUnlockStatementSql);
      stmt.setString(1, key);
      stmt.setString(2, handle.getUuid());
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw e;
    } finally {
      if (stmt != null)
        stmt.close();
      if (connection != null)
        connection.close();
    }
  }

}
