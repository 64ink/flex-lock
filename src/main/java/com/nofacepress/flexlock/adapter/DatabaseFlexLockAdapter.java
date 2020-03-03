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

import java.math.BigDecimal;
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
public class DatabaseFlexLockAdapter<KeyType> implements FlexLockAdapter<KeyType> {

  private static class SQL {
    static final String TABLE_KEY = "[MUTEX_TABLE]";
    static final String PRIMARY_KEY = "[PRIMARY_KEY]";
    static final String EXPIRE_TIME = "[EXPIRE_TIME]";
    static final String OWNER = "[OWNER]";
    static final String TRY_LOCK_UPDATE = "update [MUTEX_TABLE] set [OWNER]=?, [EXPIRE_TIME]=? where [PRIMARY_KEY]=? and [EXPIRE_TIME]<=?";
    static final String MUTEX_EXISTS = "select 1 from [MUTEX_TABLE] where [PRIMARY_KEY]=?";
    static final String INSERT_MUTEX = "insert into [MUTEX_TABLE] ([PRIMARY_KEY], [EXPIRE_TIME]) values (?, 0)";
    static final String TRY_UNLOCK_UPDATE = "update [MUTEX_TABLE] set [EXPIRE_TIME]=0 where [PRIMARY_KEY]=? and [OWNER]=?";
    static final String FORCE_UNLOCK_UPDATE = "update [MUTEX_TABLE] set [EXPIRE_TIME]=0 where [PRIMARY_KEY]=?";
  }

  static interface PrimaryKeyStatementSetter<KeyType> {
    void setPrimaryKeyIntStatement(PreparedStatement stmt, int parameterIndex, KeyType value) throws SQLException;
  }

  public static final String DEFAULT_TABLE_NAME = "virtual_mutexes";
  public static final String DEFAULT_PRIMARY_KEY = "mutex_id";
  public static final String DEFAULT_EXPIRE_TIME_COL = "expire_time";
  public static final String DEFAULT_OWNER_COL = "owner";
  private static final int MAX_PREPARED_STATEMENTS = 20;

  private final BasicDataSource connectionPool;
  private final String tryLockStatementSql;
  private final String mutexExistsStatementSql;
  private final String insertMutexStatementSql;
  private final String tryUnlockStatementSql;
  private final String forceUnlockStatementSql;

  private PrimaryKeyStatementSetter<KeyType> primaryKeyStatementSetter = null;

  public DatabaseFlexLockAdapter(final String dbDriver, final String dbUrl, final String dbUser,
      final String dbPassword) throws SQLException, ClassNotFoundException {
    this(dbDriver, dbUrl, dbUser, dbPassword, DEFAULT_TABLE_NAME, DEFAULT_PRIMARY_KEY, DEFAULT_EXPIRE_TIME_COL,
        DEFAULT_OWNER_COL);
  }

  public DatabaseFlexLockAdapter(final String dbDriver, final String dbUrl, final String dbUser,
      final String dbPassword, final String tableName) throws SQLException, ClassNotFoundException {
    this(dbDriver, dbUrl, dbUser, dbPassword, tableName, DEFAULT_PRIMARY_KEY, DEFAULT_EXPIRE_TIME_COL,
        DEFAULT_OWNER_COL);
  }

  public DatabaseFlexLockAdapter(final String dbDriver, final String dbUrl, final String dbUser,
      final String dbPassword, final String tableName, final String primaryKeyName, final String expiresColumnName,
      final String ownerColumnName) throws SQLException, ClassNotFoundException {

    tryLockStatementSql = SQL.TRY_LOCK_UPDATE.replace(SQL.TABLE_KEY, tableName).replace(SQL.PRIMARY_KEY, primaryKeyName)
        .replace(SQL.EXPIRE_TIME, expiresColumnName).replace(SQL.OWNER, ownerColumnName);
    mutexExistsStatementSql = SQL.MUTEX_EXISTS.replace(SQL.TABLE_KEY, tableName)
        .replace(SQL.PRIMARY_KEY, primaryKeyName).replace(SQL.EXPIRE_TIME, expiresColumnName)
        .replace(SQL.OWNER, ownerColumnName);
    insertMutexStatementSql = SQL.INSERT_MUTEX.replace(SQL.TABLE_KEY, tableName)
        .replace(SQL.PRIMARY_KEY, primaryKeyName).replace(SQL.EXPIRE_TIME, expiresColumnName)
        .replace(SQL.OWNER, ownerColumnName);
    tryUnlockStatementSql = SQL.TRY_UNLOCK_UPDATE.replace(SQL.TABLE_KEY, tableName)
        .replace(SQL.PRIMARY_KEY, primaryKeyName).replace(SQL.EXPIRE_TIME, expiresColumnName)
        .replace(SQL.OWNER, ownerColumnName);
    forceUnlockStatementSql = SQL.FORCE_UNLOCK_UPDATE.replace(SQL.TABLE_KEY, tableName)
        .replace(SQL.PRIMARY_KEY, primaryKeyName).replace(SQL.EXPIRE_TIME, expiresColumnName)
        .replace(SQL.OWNER, ownerColumnName);

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
   * @see
   * com.dtis.common.mutex.MutexAdapter#ensureKeyExistsCreatingIfNessessary(java.
   * lang.String)
   */
  public void ensureKeyExistsCreatingIfNessessary(final KeyType key) throws Exception {
    PreparedStatement stmt = null;
    Connection connection = null;
    try {
      connection = connectionPool.getConnection();
      stmt = connection.prepareStatement(mutexExistsStatementSql);
      setPrimaryKeyInStatement(stmt, 1, key);
      final ResultSet results = stmt.executeQuery();
      if (!results.next()) {
        // need to insert it
        stmt.close();
        stmt = null;
        stmt = connection.prepareStatement(insertMutexStatementSql);
        setPrimaryKeyInStatement(stmt, 1, key);
        stmt.executeUpdate();
      }
    } catch (final SQLException e) {
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
  public void forceUnlock(final KeyType key) throws Exception {
    PreparedStatement stmt = null;
    Connection connection = null;
    try {
      connection = connectionPool.getConnection();
      stmt = connection.prepareStatement(forceUnlockStatementSql);
      setPrimaryKeyInStatement(stmt, 1, key);
      stmt.executeUpdate();
    } catch (final SQLException e) {
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
  public boolean tryLock(final KeyType key, final FlexLockHandle handle, final long now, final long expireTime)
      throws Exception {
    PreparedStatement stmt = null;
    Connection connection = null;
    try {
      connection = connectionPool.getConnection();
      stmt = connection.prepareStatement(tryLockStatementSql);
      stmt.setString(1, handle.getUuid());
      stmt.setLong(2, expireTime);
      setPrimaryKeyInStatement(stmt, 3, key);
      stmt.setLong(4, now);
      final int updates = stmt.executeUpdate();
      return updates > 0;
    } catch (final SQLException e) {
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
  public void unlock(final KeyType key, final FlexLockHandle handle) throws Exception {
    PreparedStatement stmt = null;
    Connection connection = null;
    try {
      connection = connectionPool.getConnection();
      stmt = connection.prepareStatement(tryUnlockStatementSql);
      setPrimaryKeyInStatement(stmt, 1, key);
      stmt.setString(2, handle.getUuid());
      stmt.executeUpdate();
    } catch (final SQLException e) {
      throw e;
    } finally {
      if (stmt != null)
        stmt.close();
      if (connection != null)
        connection.close();
    }
  }

  private void setPrimaryKeyInStatement(PreparedStatement stmt, int parameterIndex, KeyType value) throws SQLException {
    if (primaryKeyStatementSetter == null) {

      if (value instanceof Long) {
        primaryKeyStatementSetter = (PreparedStatement v_stmt, int v_parameterIndex, KeyType v_value) -> v_stmt
            .setLong(v_parameterIndex, Long.class.cast(v_value));

      }

      else if (value instanceof Integer) {
        primaryKeyStatementSetter = (PreparedStatement v_stmt, int v_parameterIndex, KeyType v_value) -> v_stmt
            .setInt(v_parameterIndex, Integer.class.cast(v_value));
      }

      else if (value instanceof Float) {
        primaryKeyStatementSetter = (PreparedStatement v_stmt, int v_parameterIndex, KeyType v_value) -> v_stmt
            .setFloat(v_parameterIndex, Float.class.cast(v_value));
      }

      else if (value instanceof Double) {
        primaryKeyStatementSetter = (PreparedStatement v_stmt, int v_parameterIndex, KeyType v_value) -> v_stmt
            .setDouble(v_parameterIndex, Double.class.cast(v_value));
      }

      else if (value instanceof BigDecimal) {
        primaryKeyStatementSetter = (PreparedStatement v_stmt, int v_parameterIndex, KeyType v_value) -> v_stmt
            .setBigDecimal(v_parameterIndex, BigDecimal.class.cast(v_value));
      }

      else if (value instanceof String) {
        primaryKeyStatementSetter = (PreparedStatement v_stmt, int v_parameterIndex, KeyType v_value) -> v_stmt
            .setString(v_parameterIndex, String.class.cast(v_value));
      }

      else {
        primaryKeyStatementSetter = (PreparedStatement v_stmt, int v_parameterIndex, KeyType v_value) -> v_stmt
            .setString(v_parameterIndex, v_value.toString());
      }

    }

    primaryKeyStatementSetter.setPrimaryKeyIntStatement(stmt, parameterIndex, value);
  }
}
