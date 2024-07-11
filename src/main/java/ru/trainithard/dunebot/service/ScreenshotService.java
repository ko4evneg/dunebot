package ru.trainithard.dunebot.service;

import java.io.IOException;

@Deprecated(since = "v0.4.60")
public interface ScreenshotService {
    String save(long matchId, String dottedFileExtension, byte[] screenshot) throws IOException;
}
