-- Run this migration once on MySQL / MariaDB before starting the updated app.

ALTER TABLE `place`
    ADD COLUMN `google_synced_at` DATETIME NULL AFTER `region`;

UPDATE `place`
SET `google_synced_at` = `modified_at`
WHERE `google_synced_at` IS NULL;
