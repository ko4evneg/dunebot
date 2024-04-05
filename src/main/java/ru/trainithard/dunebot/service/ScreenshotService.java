package ru.trainithard.dunebot.service;

import java.io.IOException;

public interface ScreenshotService {
    String save(long matchId, String dottedFileExtension, byte[] screenshot) throws IOException;
}
