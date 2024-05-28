package com.agilysys.StayTenantPurger.Config;

import com.agilysys.StayTenantPurger.Service.StayDeleteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {
    @Autowired
    private StayDeleteService stayDeleteService;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Scheduled(cron = "0 59 23 ? * SUN")
    public void weeklyTaskinitiated() {
        logger.info("Weekly Task initiated");
    }
}
