
CREATE TABLE IF NOT EXISTS `TestMutex` (
	`mutex_id` VARCHAR(128) NOT NULL,
	`expire_time` LONG DEFAULT 0,
	`owner` VARCHAR(36),
	PRIMARY KEY (`mutex_id`)
);