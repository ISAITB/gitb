ALTER TABLE `testresults` ADD COLUMN `output_message` TEXT AFTER `end_time`;
ALTER TABLE `conformanceresults` ADD COLUMN `output_message` TEXT AFTER `result`;