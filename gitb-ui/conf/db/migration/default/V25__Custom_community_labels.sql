DROP TABLE IF EXISTS `communitylabels`;
CREATE TABLE `communitylabels` (
  `community` bigint(20) NOT NULL,
  `label_type` TINYINT NOT NULL,
  `singular_form` varchar(254) NOT NULL,
  `plural_form` varchar(254) NOT NULL,
  PRIMARY KEY (`community`, `label_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE `communitylabels` ADD CONSTRAINT `cl_fk_com` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
