--
-- Merge landing pages, legal notices and error templates into a single custom content table.
-- content_type: 1 = landing page, 2 = legal notice, 3 = error template.
--
CREATE TABLE `customcontent` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext COLLATE utf8mb4_0900_ai_ci,
  `default_flag` tinyint NOT NULL DEFAULT '0',
  `description` longtext COLLATE utf8mb4_0900_ai_ci,
  `community` bigint NOT NULL,
  `content_type` tinyint NOT NULL,
  `old_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `cc_idx_com_type` (`community`, `content_type`),
  CONSTRAINT `cc_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `customcontent` (`name`, `content`, `default_flag`, `description`, `community`, `content_type`, `old_id`)
  SELECT `name`, `content`, `default_flag`, `description`, `community`, 1, `id` FROM `landingpages`;
INSERT INTO `customcontent` (`name`, `content`, `default_flag`, `description`, `community`, `content_type`, `old_id`)
  SELECT `name`, `content`, `default_flag`, `description`, `community`, 2, `id` FROM `legalnotices`;
INSERT INTO `customcontent` (`name`, `content`, `default_flag`, `description`, `community`, `content_type`, `old_id`)
  SELECT `name`, `content`, `default_flag`, `description`, `community`, 3, `id` FROM `errortemplates`;

ALTER TABLE `organizations` DROP FOREIGN KEY `org_fk_lanpag`;
ALTER TABLE `organizations` DROP FOREIGN KEY `org_fk_legnot`;
ALTER TABLE `organizations` DROP FOREIGN KEY `org_fk_errtem`;

UPDATE `organizations` o JOIN `customcontent` c ON c.`old_id` = o.`landing_page` AND c.`content_type` = 1 SET o.`landing_page` = c.`id`;
UPDATE `organizations` o JOIN `customcontent` c ON c.`old_id` = o.`legal_notice` AND c.`content_type` = 2 SET o.`legal_notice` = c.`id`;
UPDATE `organizations` o JOIN `customcontent` c ON c.`old_id` = o.`error_template` AND c.`content_type` = 3 SET o.`error_template` = c.`id`;

ALTER TABLE `organizations` ADD CONSTRAINT `org_fk_lanpag` FOREIGN KEY (`landing_page`) REFERENCES `customcontent` (`id`);
ALTER TABLE `organizations` ADD CONSTRAINT `org_fk_legnot` FOREIGN KEY (`legal_notice`) REFERENCES `customcontent` (`id`);
ALTER TABLE `organizations` ADD CONSTRAINT `org_fk_errtem` FOREIGN KEY (`error_template`) REFERENCES `customcontent` (`id`);

ALTER TABLE `customcontent` DROP COLUMN `old_id`;

DROP TABLE `landingpages`;
DROP TABLE `legalnotices`;
DROP TABLE `errortemplates`;
