package com.agilysys.StayTenantPurger.Config;

import com.agilysys.StayTenantPurger.Service.StayMongoDeleteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ScheduledTasks {
    @Autowired
    private StayMongoDeleteService stayMongoDeleteService;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private String[] env = new String[]{"000", "005"};

    @Scheduled(cron = "0 0 3 * * ?")
    public void weeklyTaskinitiated() {
        logger.info("Daily Task initiated");
        Arrays.stream(env).forEach(x -> {
            stayMongoDeleteService.deleteInMongodb(x, true);
            logger.info("The {} environment cleaned up",x);
        });
    }
}
