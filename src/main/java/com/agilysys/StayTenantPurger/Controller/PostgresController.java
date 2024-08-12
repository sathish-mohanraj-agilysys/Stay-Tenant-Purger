package com.agilysys.StayTenantPurger.Controller;

import com.agilysys.StayTenantPurger.Interfaces.StayPostgressInterface;
import com.agilysys.StayTenantPurger.Service.PostgressDeleteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class PostgresController implements StayPostgressInterface {
    @Autowired
    PostgressDeleteService postgressDeleteService;

    @Override
    public List<String> checkTenantIdColumn(String env) {
        return postgressDeleteService.checkForTenantId(env);
    }

    @Override
    public Map<String, String> getTotalCount(String env) {
        return postgressDeleteService.getTotalRowCountInDb(env);
    }


    @Override
    public Map<String, Integer> checkTotalRowCount(String env, List<String> tenantIds) {
        return postgressDeleteService.countDocuments(env,tenantIds);
    }

    @Override
    public Map<String, Integer> deleteTenant(String env, List<String> tenantIds) {
        return postgressDeleteService.deleteTenant(env,tenantIds);
    }
}
