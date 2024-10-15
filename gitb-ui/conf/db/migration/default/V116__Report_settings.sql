CREATE TABLE `communityreportsettings` (
  `report_type` SMALLINT NOT NULL,
  `sign_pdf` TINYINT DEFAULT 0 NOT NULL,
  `custom_pdf` TINYINT DEFAULT 0 NOT NULL,
  `custom_pdf_with_custom_xml` TINYINT DEFAULT 0 NOT NULL,
  `custom_pdf_service` VARCHAR(255),
  `community` BIGINT NOT NULL,
  PRIMARY KEY (`report_type`,`community`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `communityreportsettings` ADD CONSTRAINT `crs_fk_com` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
