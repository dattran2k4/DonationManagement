package com.chiaseyeuthuong.schedule;

import com.chiaseyeuthuong.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "EVENT-STATUS-SCHEDULER")
public class EventStatusScheduler {

    private final EventService eventService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void syncEventStatusesDaily() {
        int changed = eventService.syncStatusesBySchedule();
        if (changed > 0) {
            log.info("Auto-synced event statuses. Changed {} event(s).", changed);
        } else {
            log.info("Auto-synced event statuses. No changes.");
        }
    }
}

