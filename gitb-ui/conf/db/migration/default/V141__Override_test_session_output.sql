ALTER TABLE `testresultcomments` ADD COLUMN `output_message_forced` TEXT;
ALTER TABLE `testresultcomments` ADD COLUMN `output_message_original` TEXT;
-- Update existing comments
UPDATE `testresultcomments` trc JOIN `testresults` tr ON tr.`test_session_id` = trc.`test_session_id`
SET trc.`output_message_original` = tr.`output_message`
WHERE trc.`result_forced` IS NOT NULL;
