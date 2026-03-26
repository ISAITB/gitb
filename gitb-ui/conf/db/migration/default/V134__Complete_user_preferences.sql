-- Add missing user preferences
INSERT INTO `userpreferences`(`user`) SELECT `id` FROM `users` WHERE `id` NOT IN (SELECT `user` FROM `userpreferences`);
