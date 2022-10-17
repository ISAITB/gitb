ALTER TABLE `parameters` ADD COLUMN `test_key` varchar(254);
UPDATE `parameters` set `test_key` = `name` WHERE `test_key` is null;
ALTER TABLE `parameters` MODIFY `test_key` VARCHAR(254) NOT NULL;