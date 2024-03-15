package ru.trainithard.dunebot.service;

import java.io.IOException;

public interface ScreenshotService {
    void save(long matchId, String dottedFileExtension, byte[] screenshot) throws IOException;
}
