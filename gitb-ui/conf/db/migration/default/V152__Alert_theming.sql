-- Create columns
ALTER TABLE `themes` ADD COLUMN `alert_info_background_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `alert_info_text_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `alert_info_border_color` VARCHAR(254);
-- Migrate existing themes
UPDATE `themes` SET `alert_info_background_color` = '#d9edf7' WHERE `alert_info_background_color` IS NULL;
UPDATE `themes` SET `alert_info_text_color` = '#005885' WHERE `alert_info_text_color` IS NULL;
UPDATE `themes` SET `alert_info_border_color` = '#bddded' WHERE `alert_info_border_color` IS NULL;
-- Add constraints
ALTER TABLE `themes` MODIFY `alert_info_background_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `alert_info_text_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `alert_info_border_color` VARCHAR(254) NOT NULL;
