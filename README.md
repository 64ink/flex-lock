# Flex Lock

Flexible lock interface for locking shared network resources using a database or for simple locking with-in an application.

 Build status: [![build_status](https://travis-ci.org/nofacepress/flex-lock.svg?branch=master)](https://travis-ci.org/nofacepress/flex-lock)

## Maven Setup

```xml
<dependency>
  <groupId>com.nofacepress</groupId>
  <artifactId>flex-lock</artifactId>
  <version>0.0.2</version>
</dependency>
```

## Usage Example

```java
		FlexLockRegistry registry = new DatabaseFlexLockRegistry(DB_DRIVER, DB_URL, DB_USER, DB_PASSWORD, DB_TABLE_NAME);
		FlexLockHandle handle = registry.lock("key", 1000);
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
