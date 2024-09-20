package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.Controller.EsController;
import com.agilysys.StayTenantPurger.Controller.MongoController;
import com.agilysys.StayTenantPurger.Controller.PostgresController;
import com.agilysys.StayTenantPurger.Util.Status;
import com.agilysys.StayTenantPurger.modal.DAO.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
@Service
public class MainService {
    private static final Logger logger = LoggerFactory.getLogger(MainService.class);

    @Autowired
    EsController esController;
    @Autowired
    MongoController mongoController;
    @Autowired
    PostgresController postgresController;

    public Map<String, Object> completeDeleteTenant(Tenant tenant, String env) {
        if(tenant.getTenant().isEmpty()){
            logger.error("[{}] NUll data present in the deletion ", Status.FAILED);
            return null;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        Map<String, Object> combinedResponse = new ConcurrentHashMap<>();

        try {
            Future<ResponseEntity> mongoFuture = null;
            try {
                mongoFuture = executorService.submit(() -> {
                    mongoController.clearDataInLocal(env);
                    mongoController.addTenantInCache(tenant, env);
                    return mongoController.startDeleting(env, false);
                });
            } catch (Exception e) {
                combinedResponse.put("mongoResponse",e.getMessage());
            }

            Future<Map<String, Integer>> postgresFuture = null;
            try {
                postgresFuture = executorService.submit(() -> {
                    return postgresController.deleteTenant(env, tenant.getTenant().stream().toList());
                });
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            Future<Map<String, Map<String, String>>> esFuture = null;
            try {
                esFuture = executorService.submit(() -> {
                    return esController.deleteTenant(env, tenant.getTenant().stream().toList());
                });
            } catch (Exception e) {
                combinedResponse.put("esResponse", e.getMessage());
            }

            // Combine results
            combinedResponse.put("mongoResponse", mongoFuture.get().getBody());
            combinedResponse.put("esResponse", esFuture.get());
            try {
                combinedResponse.put("postgresResponse", postgresFuture.get());
            } catch (InterruptedException e) {
               combinedResponse.put("postgresResponse",e.getMessage());
            } catch (ExecutionException e) {
                combinedResponse.put("postgresResponse",e.getMessage());
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error while deleting tenant: {}", e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
        } finally {
            executorService.shutdown();
        }

        return combinedResponse;
    }
}
