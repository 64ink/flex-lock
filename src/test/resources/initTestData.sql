
CREATE TABLE IF NOT EXISTS `TestMutex` (
	`mutex_id` VARCHAR(128) NOT NULL,
	`expire_time` LONG DEFAULT 0,
	`owner` VARCHAR(36),
	PRIMARY KEY (`mutex_id`)
);

CREATE TABLE IF NOT EXISTS `TestLongMutex` (
	`alt_id` Long NOT NULL,
	`alt_expires` LONG DEFAULT 0,
	`alt_owner` VARCHAR(36),
	PRIMARY KEY (`alt_id`)
);