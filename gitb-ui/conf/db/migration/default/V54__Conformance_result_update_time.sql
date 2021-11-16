ALTER TABLE `conformanceresults` ADD COLUMN `update_time` TIMESTAMP;
-- Set values for existing data.
UPDATE `conformanceresults` cr
SET `update_time` = (SELECT `end_time` FROM `testresults` WHERE `test_session_id` = cr.`test_session_id`)
WHERE cr.`test_session_id` IS NOT NULL AND cr.`update_time` IS NULL;
UPDATE `conformanceresults` cr
SET `update_time` = (SELECT `start_time` FROM `testresults` WHERE `test_session_id` = cr.`test_session_id`)
WHERE cr.`test_session_id` IS NOT NULL AND cr.`update_time` IS NULL;
