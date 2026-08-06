-- Create columns
ALTER TABLE `themes` ADD COLUMN `welcome_login_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `welcome_login_label_color` VARCHAR(254);
ALTER TABLE `themes` ADD COLUMN `welcome_option_label_color` VARCHAR(254);
-- Migrate existing themes
UPDATE `themes` SET `welcome_login_color` = `primary_btn_color` WHERE `welcome_login_color` IS NULL;
UPDATE `themes` SET `welcome_login_label_color` = `primary_btn_label_color` WHERE `welcome_login_label_color` IS NULL;
UPDATE `themes` SET `welcome_option_label_color` = '#777777' WHERE `welcome_option_label_color` IS NULL;
-- Add constraints
ALTER TABLE `themes` MODIFY `welcome_login_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `welcome_login_label_color` VARCHAR(254) NOT NULL;
ALTER TABLE `themes` MODIFY `welcome_option_label_color` VARCHAR(254) NOT NULL;
