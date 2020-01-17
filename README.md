# Flex Lock

This is a sprint boot project that provide a method to lock shared network resources using a database with a flexible interface that allows for simple locking with-in an application as well.

 Build status: [![build_status](https://travis-ci.org/nofacepress/flex-lock.svg?branch=master)](https://travis-ci.org/nofacepress/flex-lock)

## Maven Setup

```xml
<dependency>
  <groupId>com.nofacepress</groupId>
  <artifactId>flex-lock</artifactId>
  <version>0.0.1</version>
</dependency>
```

## Usage Example

```java
		VirtualMutexRegistry registry = new DatabaseMutexRegistry(DB_DRIVER, DB_URL, DB_USER, DB_PASSWORD, DB_TABLE_NAME);
		VirtualMutexHandle handle = registry.lock("key", 1000);
		registry.unlock(handle);
```

## Database Setup

```sql
CREATE TABLE IF NOT EXISTS `AnyTableName` (
	`mutex_id` VARCHAR(128) NOT NULL,
	`expire_time` LONG DEFAULT 0,
	`owner` VARCHAR(36),
	PRIMARY KEY (`mutex_id`)
);
```
