CREATE TABLE `testflags` (
  `id` BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `colour` varchar(254) NOT NULL,
  `public_name` varchar(254),
  `public_colour` varchar(254),
  `admin_only` tinyint NOT NULL DEFAULT 0,
  `display_order` smallint NOT NULL,
  `community` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX `testflag_community_idx` ON `testflags` (`community`);

ALTER TABLE `testflags` ADD CONSTRAINT `testflag_fk_community` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);

ALTER TABLE `testresults` ADD COLUMN `flag_id` BIGINT AFTER `output_message`;

CREATE INDEX `tr_idx_flag` ON `testresults` (`flag_id`);

ALTER TABLE `testresults` ADD CONSTRAINT `tr_fk_flag` FOREIGN KEY (`flag_id`) REFERENCES `testflags`(`id`);
