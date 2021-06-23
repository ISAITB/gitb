ALTER TABLE `testcases` ADD COLUMN `testsuite_order` SMALLINT;
-- The order can be set to the ID at migration time as this will match their current ordering.
UPDATE `testcases` SET `testsuite_order` = `id`;
ALTER TABLE `testcases` MODIFY `testsuite_order` SMALLINT NOT NULL;