-- An empty welcome message value now means the message is not displayed, whereas it was
-- previously ignored in favour of the built-in default. Delete any such existing entries
-- so that the default keeps being displayed for them.
DELETE FROM `systemconfigurations` WHERE `name` = 'welcome' AND (`parameter` IS NULL OR `parameter` = '');
