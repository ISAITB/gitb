CREATE TABLE `testresultdefinitions` (
  `test_session_id` varchar(254) COLLATE utf8mb4_bin PRIMARY KEY NOT NULL,
  `tpl` blob NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `testresultdefinitions` ADD CONSTRAINT `trd_fk_tr` FOREIGN KEY (`test_session_id`) REFERENCES `testresults`(`test_session_id`);
-- Copy existing data.
INSERT INTO `testresultdefinitions` (`test_session_id`, `tpl`) SELECT `test_session_id`, `tpl` FROM `testresults`;
-- Delete original column.
ALTER TABLE `testresults` DROP COLUMN `tpl`;