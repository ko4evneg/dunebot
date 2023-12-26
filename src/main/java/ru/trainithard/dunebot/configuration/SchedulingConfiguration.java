package ru.trainithard.dunebot.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Profile(value = {"!test"})
@EnableScheduling
@Configuration
public class SchedulingConfiguration {
}
