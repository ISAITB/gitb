DELETE FROM `configurations` WHERE `system` NOT IN (SELECT `id` FROM `systems`) OR `parameter` NOT IN (SELECT `id` FROM `parameters`) OR `endpoint` NOT IN (SELECT `id` FROM `endpoints`);
ALTER TABLE `configurations` ADD CONSTRAINT `co_fk_sys` FOREIGN KEY (`system`) REFERENCES `systems`(`id`);
ALTER TABLE `configurations` ADD CONSTRAINT `co_fk_par` FOREIGN KEY (`parameter`) REFERENCES `parameters`(`id`);
ALTER TABLE `configurations` ADD CONSTRAINT `co_fk_end` FOREIGN KEY (`endpoint`) REFERENCES `endpoints`(`id`);
CREATE INDEX `co_fk_sys` on `configurations`(`system`);
CREATE INDEX `co_fk_par` on `configurations`(`parameter`);
CREATE INDEX `co_fk_end` on `configurations`(`endpoint`);

DELETE FROM `parameters` WHERE `endpoint` NOT IN (SELECT `id` FROM `endpoints`);
ALTER TABLE `parameters` ADD CONSTRAINT `pa_fk_end` FOREIGN KEY (`endpoint`) REFERENCES `endpoints`(`id`);

ALTER TABLE `parameters` ADD COLUMN `admin_only` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `parameters` ADD COLUMN `not_for_tests` TINYINT DEFAULT 0 NOT NULL;