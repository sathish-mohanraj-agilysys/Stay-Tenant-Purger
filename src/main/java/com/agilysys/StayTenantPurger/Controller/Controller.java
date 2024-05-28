package com.agilysys.StayTenantPurger.Controller;

import com.agilysys.StayTenantPurger.DAO.Tenant;
import com.agilysys.StayTenantPurger.Interfaces.StayDeleteInterface;
import com.agilysys.StayTenantPurger.Service.CoreDeleteSerive;
import com.agilysys.StayTenantPurger.Service.StayDeleteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller implements StayDeleteInterface {

    @Autowired
    private CoreDeleteSerive coreDeleteSerive;
    @Autowired
    private StayDeleteService stayDeleteService;

    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(MongoTemplate mongoTemplate) {

    }

    public String deleteTenant(@RequestBody Tenant tenant, @PathVariable("environment") String env) {
        return stayDeleteService.storData(tenant, env);
    }

    public ResponseEntity startDeleting(@PathVariable("environment") String env) {
        return stayDeleteService.deleteInMongodb(env);
    }

    public String clearData(@PathVariable("environment") String env) {
        return stayDeleteService.clearInLocal(env);
    }

    public String getData(@PathVariable("environment") String env) {
        return stayDeleteService.getDataFromCache(env);
    }

    public String dropCollections(@PathVariable("environment") String env) {
        return stayDeleteService.dropAllCollections(env);
    }
}

