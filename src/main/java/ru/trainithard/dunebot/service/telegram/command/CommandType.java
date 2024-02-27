package ru.trainithard.dunebot.service.telegram.command;

/**
 * Type of external command, based on its source.
 */
public enum CommandType {
    /**
     * Text sourced command
     */
    TEXT,
    /**
     * Poll vote sourced command
     */
    POLL_VOTE,
    /**
     * Pressed button sourced command
     */
    CALLBACK,
    /**
     * File upload sourced command
     */
    FILE_UPLOAD
}
