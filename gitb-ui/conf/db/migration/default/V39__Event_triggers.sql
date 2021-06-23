CREATE TABLE `triggers` (
  `id` BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `url` varchar(254) NOT NULL,
  `event_type` tinyint NOT NULL,
  `operation` varchar(254),
  `active` tinyint NOT NULL DEFAULT 1,
  `latest_result_ok` tinyint,
  `latest_result_output` text,
  `community` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `triggerdata` (
  `data_type` tinyint NOT NULL,
  `data_id` BIGINT NOT NULL,
  `trigger` BIGINT NOT NULL,
  PRIMARY KEY (`data_type`,`data_id`,`trigger`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX `trigger_community_idx` ON `triggers` (`community`);
CREATE INDEX `trigger_data_idx` ON `triggerdata` (`trigger`);
CREATE INDEX `trigger_data_id_idx` ON `triggerdata` (`data_id`);
CREATE INDEX `trigger_data_id_type_idx` ON `triggerdata` (`data_id`, `data_type`);

ALTER TABLE `triggers` ADD CONSTRAINT `trigger_fk_community` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
ALTER TABLE `triggerdata` ADD CONSTRAINT `triggerdata_fk_trigger` FOREIGN KEY (`trigger`) REFERENCES `triggers`(`id`);