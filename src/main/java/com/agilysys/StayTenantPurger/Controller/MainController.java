package com.agilysys.StayTenantPurger.Controller;

import com.agilysys.StayTenantPurger.Interfaces.MainInterface;
import com.agilysys.StayTenantPurger.Service.MainService;
import com.agilysys.StayTenantPurger.modal.DAO.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class MainController implements MainInterface {
    @Autowired
    MainService mainService;


    @Override
    public Map<String, Object> startDeletingSync(Tenant tenant, String env) {
        return mainService.completeDeleteTenant(tenant, env);
    }

    @Override
    public ResponseEntity<String> startDeletingAsync(Tenant tenant, String env) {
        ExecutorService executorService= Executors.newFixedThreadPool(3);
        executorService.submit(()-> mainService.completeDeleteTenant(tenant, env));
        return ResponseEntity.of("Accecpted".describeConstable());
    }
}
