--
-- Add support for a community-specific report file name naming expression override.
--
ALTER TABLE `communityreportsettings` ADD COLUMN `file_name_expression` VARCHAR(512) NULL;
