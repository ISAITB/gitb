-- Create columns
ALTER TABLE `themes` ADD COLUMN `primary_btn_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `primary_btn_label_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `primary_btn_hover_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `primary_btn_active_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `secondary_btn_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `secondary_btn_label_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `secondary_btn_hover_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `secondary_btn_active_color` VARCHAR(254);
-- Migrate existing themes
UPDATE `themes` SET `primary_btn_color` = '#337ab7' WHERE `primary_btn_color` IS NULL;
UPDATE `themes` SET `primary_btn_label_color` = '#FFFFFF' WHERE `primary_btn_label_color` IS NULL;
UPDATE `themes` SET `primary_btn_hover_color` = '#2b689c' WHERE `primary_btn_hover_color` IS NULL;
UPDATE `themes` SET `primary_btn_active_color` = '#296292' WHERE `primary_btn_active_color` IS NULL;
UPDATE `themes` SET `secondary_btn_color` = '#6c757d' WHERE `secondary_btn_color` IS NULL;
UPDATE `themes` SET `secondary_btn_label_color` = '#FFFFFF' WHERE `secondary_btn_label_color` IS NULL;
UPDATE `themes` SET `secondary_btn_hover_color` = '#5c636a' WHERE `secondary_btn_hover_color` IS NULL;
UPDATE `themes` SET `secondary_btn_active_color` = '#565e64' WHERE `secondary_btn_active_color` IS NULL;
-- Add constraints
ALTER TABLE `themes` MODIFY `primary_btn_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `primary_btn_label_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `primary_btn_hover_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `primary_btn_active_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `secondary_btn_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `secondary_btn_label_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `secondary_btn_hover_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `secondary_btn_active_color` VARCHAR(254) NOT NULL;