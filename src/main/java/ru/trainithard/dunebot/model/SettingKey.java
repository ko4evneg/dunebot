package ru.trainithard.dunebot.model;

/**
 * Setting key for receiving setting values from storage
 */
public enum SettingKey {
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
     * Timeout for started match - ends match if no submit was done before this timeout
     */
    FINISH_MATCH_TIMEOUT,
    /**
     * Limit of resubmit match attempts
     */
    RESUBMITS_LIMIT,
    /**
     * Monthly threshold of required matches for rating participation
     */
    MONTHLY_MATCHES_THRESHOLD;
}
