CREATE INDEX `tr_idx_start_time` ON `testresults`(`start_time` ASC);
CREATE INDEX `tr_idx_end_time_start_time` ON `testresults`(`end_time`, `start_time`);
CREATE INDEX `tr_idx_community_end_time` ON `testresults`(`community_id`, `end_time` DESC);
CREATE INDEX `tr_idx_community_end_time_start_time` ON `testresults`(`community_id`, `end_time`, `start_time`);