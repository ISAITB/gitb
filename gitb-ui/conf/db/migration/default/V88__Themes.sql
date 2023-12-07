-- Themes table.
CREATE TABLE `themes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `theme_key` VARCHAR(254) NOT NULL,
  `description` TEXT,
  `active` TINYINT DEFAULT 0 NOT NULL,
  `custom` TINYINT DEFAULT 0 NOT NULL,
  `separator_title_color` VARCHAR(254) NOT NULL,
  `modal_title_color` VARCHAR(254) NOT NULL,
  `table_title_color` VARCHAR(254) NOT NULL,
  `card_title_color` VARCHAR(254) NOT NULL,
  `page_title_color` VARCHAR(254) NOT NULL,
  `heading_color` VARCHAR(254) NOT NULL,
  `tab_link_color` VARCHAR(254) NOT NULL,
  `footer_text_color` VARCHAR(254) NOT NULL,
  `header_background_color` VARCHAR(254) NOT NULL,
  `header_border_color` VARCHAR(254) NOT NULL,
  `header_separator_color` VARCHAR(254) NOT NULL,
  `header_logo_path` VARCHAR(254) NOT NULL,
  `footer_background_color` VARCHAR(254) NOT NULL,
  `footer_border_color` VARCHAR(254) NOT NULL,
  `footer_logo_path` VARCHAR(254) NOT NULL,
  `footer_logo_display` VARCHAR(254) NOT NULL,
  `favicon_path` VARCHAR(254) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `themes` ADD CONSTRAINT `unique_theme_key` UNIQUE (`theme_key`);

-- Populate default themes.
INSERT INTO `themes`(theme_key, description, `active`, `custom`, `separator_title_color`, `modal_title_color`, `table_title_color`, `card_title_color`, `page_title_color`, `heading_color`, `tab_link_color`, `footer_text_color`, `header_background_color`, `header_border_color`, `header_separator_color`, `header_logo_path`, `footer_background_color`, `footer_border_color`, `footer_logo_path`, `footer_logo_display`, `favicon_path`)
VALUES ('ec', 'European Commission theme.', 0, 0, '#337ab7', '#337ab7', '#337ab7', '#337ab7', '#FFFFFF', '#337ab7', '#337ab7', '#FFFFFF', '#0065a2', '#074a8b', '#FFFFFF', '/assets/images/ec.png', '#0065a2', '#074a8b', '/assets/images/gitb.png', 'inherit', '/assets/images/favicon-ec.gif');
INSERT INTO `themes`(theme_key, description, `active`, `custom`, `separator_title_color`, `modal_title_color`, `table_title_color`, `card_title_color`, `page_title_color`, `heading_color`, `tab_link_color`, `footer_text_color`, `header_background_color`, `header_border_color`, `header_separator_color`, `header_logo_path`, `footer_background_color`, `footer_border_color`, `footer_logo_path`, `footer_logo_display`, `favicon_path`)
VALUES ('gitb', 'Default GITB theme.', 0, 0, '#337ab7', '#337ab7', '#337ab7', '#337ab7', '#FFFFFF', '#337ab7', '#337ab7', '#FFFFFF', '#171717', '#CCCCCC', '#FFFFFF', '/assets/images/gitb.png', '#121214', '#CCCCCC', '/assets/images/gitb.png', 'none', '/assets/images/favicon.png');
