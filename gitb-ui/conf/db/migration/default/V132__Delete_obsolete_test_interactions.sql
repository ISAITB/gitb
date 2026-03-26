DELETE `ti` FROM `testinteractions` AS `ti` JOIN `testresults` AS `tr` ON `ti`.`test_session_id` = `tr`.`test_session_id` WHERE `tr`.`end_time` IS NOT NULL;
