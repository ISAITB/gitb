--
-- Add support for including optional tests in PDF reports and certificates.
--
ALTER TABLE `communityreportsettings` ADD COLUMN `include_optional_tests` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `conformancecertificates` ADD COLUMN `include_optional_tests` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `conformanceoverviewcertificates` ADD COLUMN `include_optional_tests` TINYINT NOT NULL DEFAULT 0;
