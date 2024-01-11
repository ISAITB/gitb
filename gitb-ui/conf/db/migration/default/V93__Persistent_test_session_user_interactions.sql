CREATE TABLE `testinteractions` (
  `test_session_id` VARCHAR(254) COLLATE utf8mb4_bin NOT NULL,
  `test_step_id` VARCHAR(254) COLLATE utf8mb4_bin NOT NULL,
  `is_admin` TINYINT DEFAULT 0 NOT NULL,
  `tpl` TEXT NOT NULL,
  PRIMARY KEY (`test_session_id`,`test_step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `testinteractions` ADD CONSTRAINT `ti_fk_tr` FOREIGN KEY (`test_session_id`) REFERENCES `testresults`(`test_session_id`);