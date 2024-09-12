package com.agilysys.StayTenantPurger.Controller;

import com.agilysys.StayTenantPurger.Service.StaleChecker;
import com.agilysys.StayTenantPurger.modal.DAO.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;

@Component
public class ScheduledTasks {
    @Autowired
    private MainController mainController;
    @Autowired
    private StaleChecker staleChecker;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private String[] env = new String[]{"005"};

    @PostConstruct
    private void init() {
        logger.info("[{]] Automation environment cleanup");
        weeklyTaskinitiated();
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void weeklyTaskinitiated() {
        logger.info("Daily Task initiated");
        Arrays.stream(env).forEach(env -> {
            Tenant tenant = new Tenant();
            tenant.setTenant(staleChecker.checkTenants(env, false));
            tenant.setProperty(Set.of());
            mainController.startDeletingSync(tenant, env);
            logger.info("The {} environment cleaned up", env);
        });
    }
}
