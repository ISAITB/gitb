DROP PROCEDURE IF EXISTS create_testresultcomments;

DELIMITER $$
CREATE PROCEDURE create_testresultcomments()
BEGIN
    DECLARE col_collation VARCHAR(64);

SELECT COLLATION_NAME INTO col_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'testresults'
  AND COLUMN_NAME = 'test_session_id';

SET @ddl = CONCAT('
        CREATE TABLE `testresultcomments` (
          `test_session_id` varchar(254) COLLATE ', col_collation, ' NOT NULL,
          `user_comment` LONGTEXT,
          `user_comment_time` TIMESTAMP NULL,
          `user_comment_allowed` TINYINT DEFAULT 1 NOT NULL,
          `admin_comment` LONGTEXT,
          `admin_comment_time` TIMESTAMP NULL,
          `result_forced` varchar(254),
          `result_original` varchar(254),
          PRIMARY KEY (`test_session_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `testresultcomments` ADD CONSTRAINT `trc_tr_fk`
    FOREIGN KEY (`test_session_id`) REFERENCES `testresults`(`test_session_id`);
END$$
DELIMITER ;

CALL create_testresultcomments();
DROP PROCEDURE IF EXISTS create_testresultcomments;