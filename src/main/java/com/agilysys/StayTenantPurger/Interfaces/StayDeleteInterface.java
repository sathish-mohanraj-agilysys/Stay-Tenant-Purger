package com.agilysys.StayTenantPurger.Interfaces;

import com.agilysys.StayTenantPurger.DAO.Tenant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Set;

public interface StayDeleteInterface {
    @PostMapping("/addCache/environment/{environment}")
    String addTenantInCache(@RequestBody Tenant tenant, @PathVariable("environment") String env);

    @PostMapping("/delete/environment/{environment}/start")
    ResponseEntity startDeleting(@PathVariable("environment") String env, @RequestParam(value = "toDeleteCore",required = false) boolean isToDeleteCore);

    @PostMapping("/clearCache/environment/{environment}/clear")
    String clearDataInLocal(@PathVariable("environment") String env);

    @GetMapping("/getCache/environment/{environment}")
    String getDataFromCache(@PathVariable("environment") String env);

    @GetMapping("/documentCount/environment/{environment}")
    String getDocumentCountFromCache(@PathVariable("environment") String env);

    @PostMapping("/dropCollections/environment/{environment}")
    String dropCollections(@PathVariable("environment") String env);

    @GetMapping("/allCollections/environment/{environment}")
    Set<String> getAllCollections(@PathVariable("environment") String env);

    @GetMapping("/allDocumentCounts/{environment}")
    Map<String, Integer> getDocumentCount(@PathVariable("environment") String env);

    @PostMapping("/backup/environment/{environment}")
    String backupCollection(@PathVariable("environment") String env);
}
