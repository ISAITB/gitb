UPDATE `actors` set `is_default` = 0 where `is_default` IS NULL;
ALTER TABLE `actors` MODIFY `is_default` TINYINT DEFAULT 0 NOT NULL;