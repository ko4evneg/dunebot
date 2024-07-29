package ru.trainithard.dunebot.service.report.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.model.AbstractRating;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.service.AppSettingsService;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class RatingStatsComparator implements Comparator<AbstractRating> {
    private final AppSettingsService appSettingsService;

    @Override
    public int compare(AbstractRating left, AbstractRating right) {
        int matchesThreshold = appSettingsService.getIntSetting(AppSettingKey.MONTHLY_MATCHES_THRESHOLD);
        if (left.getMatchesCount() >= matchesThreshold && right.getMatchesCount() < matchesThreshold) {
            return 1;
        } else if (right.getMatchesCount() >= matchesThreshold && left.getMatchesCount() < matchesThreshold) {
            return -1;
        }

        return (int) ((left.getEfficiency() - right.getEfficiency()) * 100);
    }
}
