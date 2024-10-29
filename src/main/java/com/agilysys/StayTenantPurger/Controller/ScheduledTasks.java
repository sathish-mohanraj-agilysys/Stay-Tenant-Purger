package com.agilysys.StayTenantPurger.Controller;

import com.agilysys.StayTenantPurger.Service.StaleChecker;
import com.agilysys.StayTenantPurger.Util.Status;
import com.agilysys.StayTenantPurger.modal.DAO.StaleCron;
import com.agilysys.StayTenantPurger.modal.DAO.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class ScheduledTasks {
    @Autowired
    private MainController mainController;
    @Autowired
    private StaleChecker staleChecker;

    @Value("${cronenv:not_found}")
    private String cronEnv;

    @Value("${includeAutomationTenant:false}")
    private Boolean includeAutomationTenant;

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private StaleCron staleCron;

    @PostConstruct
    private void init() throws IOException {

        if (!cronEnv.equalsIgnoreCase("not_found")) {
            staleCron = new StaleCron();
            staleCron.setEnvironments(new ArrayList<>(Arrays.asList(cronEnv)));
            staleCron.setIsAutomationTenantsIncluded(includeAutomationTenant);
        }

        if (includeAutomationTenant) {
            logger.info("Automation environment cleanup ENABLED for " + staleCron.getEnvironments() + " with automation tenants included");
        } else logger.info("Automation environment cleanup ENABLED for " + staleCron.getEnvironments());
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyStaleCronTask() {
        LocalDateTime startTime = LocalDateTime.now();
        logger.info("Stale cron initiated at {}", startTime);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<?> future = executorService.submit(() -> {
            try {
                staleCron.getEnvironments().forEach(env -> {
                    Tenant tenant = new Tenant();
                    tenant.setTenant(staleChecker.checkTenants(env, staleCron.getIsAutomationTenantsIncluded()));
                    tenant.setProperty(Set.of());

                    if (tenant.getTenant().isEmpty()) {
                        logger.info("[{}] Stale Deletion due to none of the stale tenants found in {} environment", Status.STOPPING_PROCESS, env);
                        return;
                    }

                    logger.info("[CRON DELETION STARTED] FOR ENVIRONMENT {} WITH {} TENANTS AND {} PROPERTIES", env, tenant.getTenant(), tenant.getProperty());
                    mainController.startDeletingSync(tenant, env);
                    logger.info("The {} environment cleaned up", env);
                });
            } catch (Exception e) {
                logger.error("Error occurred during stale tenant deletion", e);
            }
        });

        // Timeout handling: kill the task if it exceeds 1 hour
        try {
            future.get(1, TimeUnit.HOURS);  // Timeout after 1 hour
        } catch (TimeoutException e) {
            future.cancel(true);  // Interrupt the task if it exceeds 1 hour
            logger.error("Stale cron task took longer than 1 hour and was terminated at {}", LocalDateTime.now());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error occurred during the execution of the stale cron task", e);
        } finally {
            executorService.shutdownNow();  // Always shutdown the executor service
            logger.info("Stale cron task finished or terminated at {}", LocalDateTime.now());
        }
    }

}
