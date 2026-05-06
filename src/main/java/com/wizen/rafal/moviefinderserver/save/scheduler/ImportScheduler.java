package com.wizen.rafal.moviefinderserver.save.scheduler;

import com.wizen.rafal.moviefinderserver.save.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "import.scheduled.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ImportScheduler {

    private final ImportService importService;

    @Scheduled(cron = "${import.scheduled.cron:0 0 3 * * *}")
    public void scheduledImport() {
        log.info("Scheduled import triggered");
        importService.importAll();
    }
}
