# Flex Lock

Flexible lock interface for locking shared network resources using a database or for simple locking with-in an application.

 Build status: [![build_status](https://travis-ci.org/nofacepress/flex-lock.svg?branch=master)](https://travis-ci.org/nofacepress/flex-lock)

## Maven Setup

```xml
<dependency>
  <groupId>com.nofacepress</groupId>
  <artifactId>flex-lock</artifactId>
  <version>1.0.1</version>
</dependency>
```

## Usage Example

```java
		FlexLockRegistry<String> registry = new DatabaseFlexLockRegistry<String>(DB_DRIVER, DB_URL, DB_USER, DB_PASSWORD, DB_TABLE_NAME);
		FlexLockHandle<String> handle = registry.lock("key", 1000);
		registry.unlock(handle);
```

## Database Setup

### Default Setup

```sql
CREATE TABLE IF NOT EXISTS `AnyTableName` (
	`mutex_id` VARCHAR(128) NOT NULL,
	`expire_time` LONG DEFAULT 0 NOT NULL,
	`owner` VARCHAR(36),
	PRIMARY KEY (`mutex_id`)
);
```

### Integration with an existing table

In this case, the lock table is a part of an existing table.

```sql
CREATE TABLE IF NOT EXISTS `AnotherTableName` (
	`another_id` LONG NOT NULL,
	-- Other fields
	`expires` LONG DEFAULT 0 NOT NULL,
	`owner` VARCHAR(36),
	PRIMARY KEY (`another_id`)
);
```

To work with this table, specify the primary key

```java
		FlexLockRegistry<Long> registry = new DatabaseFlexLockRegistry<Long>(DB_DRIVER, DB_URL, DB_USER, DB_PASSWORD, "AnotherTableName", "another_id", "expires", "owner");
		FlexLockHandle<Long> handle = registry.lock(101L, 1000);
		registry.unlock(handle);
```

The registry keeps locks in memory.  If there is going to be a very large number of keys, it is best to use the adapter directly.

```java
    final FlexLockAdapter<Long> adapter = new DatabaseFlexLockAdapter<Long>(DB_DRIVER, DB_URL, DB_USER, DB_PASSWORD, "AnotherTableName", "another_id", "expires", "owner");
		
	final FlexLockHandle handle = new FlexLockHandle();  // used to uniquely identify the owner
	
    final long now = System.currentTimeMillis();
    final boolean success = adapter.tryLock(101L, handle, now, now + 10000);
    adapter.unlock(101L, handle);

```