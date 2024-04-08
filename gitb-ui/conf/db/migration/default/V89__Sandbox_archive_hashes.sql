-- ProcessedArchives table.
CREATE TABLE `processedarchives` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `archive_hash` VARCHAR(254) NOT NULL,
  `import_time` TIMESTAMP NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `processedarchives` ADD CONSTRAINT `unique_archive_hash` UNIQUE (`archive_hash`);