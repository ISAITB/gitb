CREATE TABLE `domainparameters` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `kind` varchar(254) NOT NULL,
  `value` blob NOT NULL,
  `domain` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `domainparameters` ADD CONSTRAINT `dp_fk_domain` FOREIGN KEY (`domain`) REFERENCES `domains`(`id`);
CREATE INDEX `dp_idx_domain` on `domainparameters`(`domain`);
CREATE UNIQUE INDEX `dp_domain_name_idx` ON `domainparameters` (`name`,`domain`);
