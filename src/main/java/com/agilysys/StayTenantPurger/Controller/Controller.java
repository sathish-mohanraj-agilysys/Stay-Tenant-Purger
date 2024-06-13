package com.agilysys.StayTenantPurger.Controller;

import com.agilysys.StayTenantPurger.DAO.Tenant;
import com.agilysys.StayTenantPurger.Interfaces.StayDeleteInterface;
import com.agilysys.StayTenantPurger.Service.CoreDeleteSerive;
import com.agilysys.StayTenantPurger.Service.StayDeleteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

@RestController
public class Controller implements StayDeleteInterface {

    @Autowired
    private CoreDeleteSerive coreDeleteSerive;
    @Autowired
    private StayDeleteService stayDeleteService;
    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(Controller.class);


    public String addTenantInCache(@RequestBody Tenant tenant, @PathVariable("environment") String env) {
        ensureCaching(env);
        return stayDeleteService.storData(tenant, env);
    }

    public ResponseEntity startDeleting(@PathVariable("environment") String env,boolean isToDeleteCore) {
        ensureCaching(env);
        return stayDeleteService.deleteInMongodb(env,isToDeleteCore);
    }

    public String clearDataInLocal(@PathVariable("environment") String env) {
        ensureCaching(env);
        return stayDeleteService.clearInLocal(env);
    }

    public String getDataFromCache(@PathVariable("environment") String env) {
        ensureCaching(env);
        return stayDeleteService.getDataFromCache(env);
    }

    @Override
    public String getDocumentCountFromCache(String env) {
        return stayDeleteService.getDocumentCountFromCacheDetails(env);
    }

    public String dropCollections(@PathVariable("environment") String env) {
        ensureCaching(env);
        return stayDeleteService.dropAllCollections(env);
    }

    public Set<String> getAllCollections(String env) {
        ensureCaching(env);
        return stayDeleteService.getAllCollections(env);
    }


    public Map<String, Integer> getDocumentCount(String env) {
        ensureCaching(env);
        return stayDeleteService.getDocumentCount(env);
    }


    public String backupCollection(String env) {
        return stayDeleteService.getDocumentCountFromCacheDetailsAndBackup(env);
    }

    private void ensureCaching(String env) {
        try {
            File resourceFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", env + ".json").toFile();
            File backUpFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "Backup", env + ".json").toFile();
            if (!resourceFile.exists()) {
                resourceFile.createNewFile();
                objectMapper.writeValue(resourceFile, new Tenant());
            }
            if (!backUpFile.exists()) {
                if(!backUpFile.createNewFile())throw new RuntimeException("Cannot proceed further");
                objectMapper.writeValue(backUpFile, new Tenant());
            }


        } catch (Exception e) {
            logger.info("Cannot ensure caching");
        }
    }
}

