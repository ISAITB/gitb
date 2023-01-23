CREATE TABLE `conformanceresults` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sut_id` BIGINT NOT NULL,
  `spec_id` BIGINT NOT NULL,
  `actor_id` BIGINT NOT NULL,
  `test_suite_id` BIGINT NOT NULL,
  `test_case_id` BIGINT NOT NULL,
  `test_session_id` varchar(254),
  `result` varchar(254) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `conformanceresults` ADD CONSTRAINT `cr_fk_sut` FOREIGN KEY (`sut_id`) REFERENCES `systems`(`id`);
ALTER TABLE `conformanceresults` ADD CONSTRAINT `cr_fk_spec` FOREIGN KEY (`spec_id`) REFERENCES `specifications`(`id`);
ALTER TABLE `conformanceresults` ADD CONSTRAINT `cr_fk_actor` FOREIGN KEY (`actor_id`) REFERENCES `actors`(`id`);
ALTER TABLE `conformanceresults` ADD CONSTRAINT `cr_fk_testsuite` FOREIGN KEY (`test_suite_id`) REFERENCES `testsuites`(`id`);
ALTER TABLE `conformanceresults` ADD CONSTRAINT `cr_fk_testcase` FOREIGN KEY (`test_case_id`) REFERENCES `testcases`(`id`);
ALTER TABLE `conformanceresults` ADD CONSTRAINT `cr_fk_testsession` FOREIGN KEY (`test_session_id`) REFERENCES `testresults`(`test_session_id`);

CREATE INDEX `cr_idx_sut` on `conformanceresults`(`sut_id`);
CREATE INDEX `cr_idx_spec` on `conformanceresults`(`spec_id`);
CREATE INDEX `cr_idx_actor` on `conformanceresults`(`actor_id`);
CREATE INDEX `cr_idx_testsuite` on `conformanceresults`(`test_suite_id`);
CREATE INDEX `cr_idx_testcase` on `conformanceresults`(`test_case_id`);
CREATE INDEX `cr_idx_testsession` on `conformanceresults`(`test_session_id`);

CREATE UNIQUE INDEX `cr_resultinfo_idx` ON `conformanceresults` (`sut_id`,`spec_id`, `actor_id`, `test_suite_id`, `test_case_id`);

-- Add data for existing test sessions
insert into conformanceresults(sut_id, spec_id, actor_id, test_suite_id, test_case_id, test_session_id, result)
select tr.sut_id, tr.specification_id, tr.actor_id, tr.testsuite_id, tr.testcase_id, tr.test_session_id, tr.result
from
	testresults tr,
	(
	select
		testresults.sut_id sut_id, testresults.specification_id specification_id, testresults.actor_id actor_id, testresults.testsuite_id testsuite_id, testresults.testcase_id testcase_id, testresults.end_time, max(testresults.test_session_id) test_session_id
	from
		testresults,
		(
		select sut_id, specification_id, actor_id, testsuite_id, testcase_id, max(end_time) max_end_time
		from testresults
		where sut_id is not null and specification_id is not null and actor_id is not null and testsuite_id is not null and testcase_id is not null
		group by sut_id, specification_id, actor_id, testsuite_id, testcase_id
		) latest
	where testresults.end_time = max_end_time
		and testresults.sut_id = latest.sut_id
		and testresults.specification_id = latest.specification_id
		and testresults.actor_id = latest.actor_id
		and testresults.testsuite_id = latest.testsuite_id
		and testresults.testcase_id = latest.testcase_id
	group by end_time, sut_id, specification_id, actor_id, testsuite_id, testcase_id
	) latest_unique_session
where
	tr.test_session_id = latest_unique_session.test_session_id;

-- Add data where test sessions have not ran
insert into conformanceresults(sut_id, spec_id, actor_id, test_suite_id, test_case_id, test_session_id, result)
select
    systemimplementsactors.sut_id, systemimplementsactors.spec_id, systemimplementsactors.actor_id, testsuitehastestcases.testsuite, testcasehasactors.testcase, null, 'UNDEFINED'
from systemimplementsactors
join testcasehasactors on testcasehasactors.actor = systemimplementsactors.actor_id
join testsuitehastestcases on testsuitehastestcases.testcase = testcasehasactors.testcase
left join testresults on (testresults.testcase_id = testcasehasactors.testcase and testresults.sut_id = systemimplementsactors.sut_id)
where
    testresults.test_session_id is null;
