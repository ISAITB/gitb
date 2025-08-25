ALTER TABLE `domainparameters` ADD COLUMN `is_test_service` TINYINT DEFAULT 0 NOT NULL;

CREATE TABLE `testservices` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `service_type` TINYINT NOT NULL,
  `api_type` TINYINT NOT NULL,
  `identifier` varchar(254),
  `version` varchar(254),
  `auth_basic_username` varchar(254),
  `auth_basic_password` varchar(254),
  `auth_token_username` varchar(254),
  `auth_token_password` varchar(254),
  `auth_token_password_type` TINYINT,
  `parameter` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `testservices` ADD CONSTRAINT `ts_dp_fk` FOREIGN KEY (`parameter`) REFERENCES `domainparameters`(`id`);
ALTER TABLE `testservices` ADD CONSTRAINT `unique_ts_dp` UNIQUE (`parameter`);
CREATE INDEX `ts_idx_dp` on `testservices`(`parameter`);
