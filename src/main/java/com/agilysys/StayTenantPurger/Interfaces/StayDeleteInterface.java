package com.agilysys.StayTenantPurger.Interfaces;

import com.agilysys.StayTenantPurger.DAO.Tenant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface StayDeleteInterface {
    @PostMapping("/delete/environment/{environment}")
    String deleteTenant(@RequestBody Tenant tenant, @PathVariable("environment") String env);

    @PostMapping("/delete/environment/{environment}/start")
    ResponseEntity startDeleting(@PathVariable("environment") String env);

    @PostMapping("/delete/environment/{environment}/clear")
    String clearData(@PathVariable("environment") String env);

    @GetMapping("/delete/environment/{environment}/")
    String getData(@PathVariable("environment") String env);

    @PostMapping("/delete/environment/{environment}/dropCollection")
    String dropCollections(@PathVariable("environment") String env);
}
