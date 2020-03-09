ALTER TABLE `testcases` ADD COLUMN `has_documentation` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `testcases` ADD COLUMN `documentation` LONGTEXT;
ALTER TABLE `testsuites` ADD COLUMN `has_documentation` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `testsuites` ADD COLUMN `documentation` LONGTEXT;