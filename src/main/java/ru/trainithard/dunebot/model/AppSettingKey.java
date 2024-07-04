package ru.trainithard.dunebot.model;

import java.util.Arrays;

/**
 * Setting key for receiving setting values from storage
 */
public enum AppSettingKey {
    /**
     * External ID of admin user
     */
    ADMIN_USER_ID,
    /**
     * External topics chat ID
     */
    CHAT_ID,
    /**
     * External topic ID for Uprising mode
     */
    TOPIC_ID_UPRISING,
    /**
     * External topic ID for Classic mode
     */
    TOPIC_ID_CLASSIC,
    /**
     * Delay in seconds, from poll became full, before match start message sent
     */
    MATCH_START_DELAY,
    /**
     * Timeout in minutes for started match - ends match if no submit was done before this timeout
     */
    FINISH_MATCH_TIMEOUT,
    /**
     * Time in minutes to wait after submit is completed until match is FINISHED.
     */
    ACCEPT_SUBMIT_TIMEOUT,
    /**
     * Time in minutes before the FINISH_MATCH_TIMEOUT, in which notification will be send about upcoming match finish.
     */
    FINISH_MATCH_NOTIFICATION_AHEAD_TIMEOUT,
    /**
     * Limit of resubmit match attempts
     */
    RESUBMITS_LIMIT,
    /**
     * Monthly threshold of required matches for rating participation
     */
    MONTHLY_MATCHES_THRESHOLD,
    /**
     * Index of next guest player
     */
    NEXT_GUEST_INDEX;

    public static AppSettingKey getByName(String settingName) {
        if (settingName == null || settingName.isBlank()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(settingKey -> settingName.equalsIgnoreCase(settingKey.name()))
                .findFirst().orElse(null);
    }
}
