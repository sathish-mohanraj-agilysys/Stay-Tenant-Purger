package com.agilysys.StayTenantPurger.Controller;

import com.agilysys.StayTenantPurger.Service.StaleChecker;
import com.agilysys.StayTenantPurger.modal.DAO.StaleCron;
import com.agilysys.StayTenantPurger.Util.Status;
import com.agilysys.StayTenantPurger.modal.DAO.Tenant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

@Component
public class ScheduledTasks {
    @Autowired
    private MainController mainController;
    @Autowired
    private StaleChecker staleChecker;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private StaleCron staleCron;
    private ObjectMapper objectMapper=new ObjectMapper();

    @PostConstruct
    private void init() throws IOException {
        if (Paths.get(System.getProperty("user.dir"), "staleCron.json").toFile().exists()) {
            logger.info("[{}] Stale configuration file found", Status.FOUND);
            staleCron = objectMapper.readValue(Paths.get(System.getProperty("user.dir"), "staleCron.json").toFile(), new TypeReference<StaleCron>() {});
        } else {
           staleCron=new StaleCron(new ArrayList<>(),false);
        }


        logger.info("Automation environment cleanup ENABLED for "+staleCron.getEnvironments());
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void weeklyTaskinitiated() {
        logger.info("Daily Task initiated");
        staleCron.getEnvironments().forEach(env -> {
            Tenant tenant = new Tenant();
            tenant.setTenant(staleChecker.checkTenants(env, staleCron.getIsAutomationTenantsIncluded()));
            tenant.setProperty(Set.of());
            logger.info("[CRON DELETION STARTED] FOR THE ENV {} WITH {} TENANTS {} PROPERTIES",env,tenant.getTenant(),tenant.getProperty());
            mainController.startDeletingSync(tenant, env);
            logger.info("The {} environment cleaned up", env);
        });
    }
}
