-- Modify tables.
ALTER TABLE `conformancesnapshottestcases` ADD COLUMN `version` varchar(254) COLLATE utf8mb4_bin NOT NULL DEFAULT "";
ALTER TABLE `conformancesnapshottestsuites` ADD COLUMN `version` varchar(254) COLLATE utf8mb4_bin NOT NULL DEFAULT "";
-- Migrate.
UPDATE `conformancesnapshottestcases` SET `version` = (SELECT `version` FROM `testcases` WHERE `testcases`.`id` = `conformancesnapshottestcases`.`id`) WHERE `id` > 0;
UPDATE `conformancesnapshottestsuites` SET `version` = (SELECT `version` FROM `testsuites` WHERE `testsuites`.`id` = `conformancesnapshottestsuites`.`id`) WHERE `id` > 0;