DROP TABLE IF EXISTS `organisationparameters`;
CREATE TABLE `organisationparameters` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `test_key` varchar(254) NOT NULL,
  `description` text,
  `use` varchar(254) NOT NULL,
  `kind` varchar(254) NOT NULL,
  `admin_only` TINYINT DEFAULT 0 NOT NULL,
  `not_for_tests` TINYINT DEFAULT 1 NOT NULL,
  `community` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `organisationparameters` ADD CONSTRAINT `op_fk_com` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
CREATE INDEX `op_idx_com` on `organisationparameters`(`community`);

DROP TABLE IF EXISTS `systemparameters`;
CREATE TABLE `systemparameters` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `test_key` varchar(254) NOT NULL,
  `description` text,
  `use` varchar(254) NOT NULL,
  `kind` varchar(254) NOT NULL,
  `admin_only` TINYINT DEFAULT 0 NOT NULL,
  `not_for_tests` TINYINT DEFAULT 1 NOT NULL,
  `community` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `systemparameters` ADD CONSTRAINT `sy_fk_com` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
CREATE INDEX `sy_idx_com` on `systemparameters`(`community`);

DROP TABLE IF EXISTS `organisationparametervalues`;
CREATE TABLE `organisationparametervalues` (
  `organisation` BIGINT NOT NULL,
  `parameter` BIGINT NOT NULL,
  `value` blob NOT NULL,
  PRIMARY KEY (`organisation`,`parameter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `organisationparametervalues` ADD CONSTRAINT `opv_fk_org` FOREIGN KEY (`organisation`) REFERENCES `organizations`(`id`);
ALTER TABLE `organisationparametervalues` ADD CONSTRAINT `opv_fk_par` FOREIGN KEY (`parameter`) REFERENCES `organisationparameters`(`id`);
CREATE INDEX `opv_idx_org` on `organisationparametervalues`(`organisation`);
CREATE INDEX `opv_idx_par` on `organisationparametervalues`(`parameter`);

DROP TABLE IF EXISTS `systemparametervalues`;
CREATE TABLE `systemparametervalues` (
  `system` BIGINT NOT NULL,
  `parameter` BIGINT NOT NULL,
  `value` blob NOT NULL,
  PRIMARY KEY (`system`,`parameter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `systemparametervalues` ADD CONSTRAINT `spv_fk_sys` FOREIGN KEY (`system`) REFERENCES `systems`(`id`);
ALTER TABLE `systemparametervalues` ADD CONSTRAINT `spv_fk_par` FOREIGN KEY (`parameter`) REFERENCES `systemparameters`(`id`);
CREATE INDEX `spv_idx_sys` on `systemparametervalues`(`system`);
CREATE INDEX `spv_idx_par` on `systemparametervalues`(`parameter`);