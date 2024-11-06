CREATE TABLE `triggerfireexpressions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `expression` varchar(1024) NOT NULL,
  `expression_type` SMALLINT NOT NULL,
  `not_match` TINYINT DEFAULT 0 NOT NULL,
  `trigger` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX `tfe_trigger_idx` ON `triggerfireexpressions` (`trigger`);
ALTER TABLE `triggerfireexpressions` ADD CONSTRAINT `tfe_fk_trigger` FOREIGN KEY (`trigger`) REFERENCES `triggers`(`id`);