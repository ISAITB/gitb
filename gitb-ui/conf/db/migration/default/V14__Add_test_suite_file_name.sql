ALTER TABLE `testsuites` ADD COLUMN `file_name` VARCHAR(255);
UPDATE `testsuites` SET `file_name` = `sname`;
ALTER TABLE `testsuites` MODIFY `file_name` VARCHAR(255) NOT NULL;