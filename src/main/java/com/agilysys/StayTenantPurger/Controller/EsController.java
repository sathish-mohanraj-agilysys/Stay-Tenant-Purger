package com.agilysys.StayTenantPurger.Controller;

import com.agilysys.StayTenantPurger.Interfaces.EsInterface;
import com.agilysys.StayTenantPurger.Service.ElasticDeleteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/es")
public class EsController implements EsInterface {
    @Autowired
    ElasticDeleteService elasticDeleteService;

    @Override
    public Object getIndexes(String env) {
        return elasticDeleteService.getIndexes(env);
    }

    @Override
    public Map<String, String> deleteTenant(String env, List<String> tenants) {
        return elasticDeleteService.startDeletingTenants(env, tenants);
    }
}
