ALTER TABLE `testcasehasactors` ADD COLUMN `sut` TINYINT DEFAULT 0 NOT NULL;
CREATE TABLE `testcasehasactors_temp` (
  `testcase` BIGINT NOT NULL,
  `specification` BIGINT NOT NULL,
  `actor` BIGINT NOT NULL,
  `sut` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`testcase`, `specification`, `actor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO `testcasehasactors_temp`(`testcase`, `specification`, `actor`, `sut`) SELECT `testcase`, `specification`, `actor`, `sut` FROM `testcasehasactors`;

-- Set role for actors that already have conformance statements.
UPDATE `testcasehasactors` SET `sut` = 1 WHERE `actor` IN (SELECT DISTINCT `actor_id` FROM `conformanceresults`);

DELETE FROM `testcasehasactors_temp`;
INSERT INTO `testcasehasactors_temp`(`testcase`, `specification`, `actor`, `sut`) SELECT `testcase`, `specification`, `actor`, `sut` FROM `testcasehasactors`;

-- Set role for actors where they are the only actors in test cases.
UPDATE `testcasehasactors` SET `sut` = 1 WHERE `testcase` IN (SELECT tc.`testcase` FROM `testcasehasactors_temp` tc GROUP BY tc.`testcase` HAVING COUNT(*) = 1);

DELETE FROM `testcasehasactors_temp`;
INSERT INTO `testcasehasactors_temp`(`testcase`, `specification`, `actor`, `sut`) SELECT `testcase`, `specification`, `actor`, `sut` FROM `testcasehasactors`;

-- Update remaining cases based on other SUT actor roles in the same specification.
UPDATE `testcasehasactors` t SET t.`sut` = 1
WHERE t.`sut` = 0
AND t.`testcase` IN (SELECT tc.`testcase` FROM `testcasehasactors_temp` tc GROUP BY tc.`testcase` HAVING SUM(tc.`sut`) = 0)
AND t.`actor` IN (SELECT DISTINCT tc2.`actor` FROM `testcasehasactors_temp` tc2 WHERE tc2.`specification` = t.`specification` and tc2.`sut` = 1);

DELETE FROM `testcasehasactors_temp`;
INSERT INTO `testcasehasactors_temp`(`testcase`, `specification`, `actor`, `sut`) SELECT `testcase`, `specification`, `actor`, `sut` FROM `testcasehasactors`;

-- Update remaining based on same actor IDs in other specifications of the domain that are marked as SUTs.
UPDATE `testcasehasactors` t SET t.`sut` = 1
WHERE t.`sut` = 0
AND t.`testcase` IN (SELECT tc.`testcase` FROM `testcasehasactors_temp` tc GROUP BY tc.`testcase` HAVING SUM(tc.`sut`) = 0)
AND t.actor IN (SELECT DISTINCT a2.`id`
  from `actors` a2 where `actorid` IN (
    SELECT distinct a.`actorid` FROM
    `actors` a
    join `domains` d on d.`id` = a.`domain`
    join `testcasehasactors_temp` tc on a.`id` = tc.`actor`
    where tc.`sut` = 1
  )
);

-- Cleanup.
DROP TABLE `testcasehasactors_temp`;