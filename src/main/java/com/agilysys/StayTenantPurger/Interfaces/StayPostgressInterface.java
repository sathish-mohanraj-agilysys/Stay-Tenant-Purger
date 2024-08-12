package com.agilysys.StayTenantPurger.Interfaces;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

public interface StayPostgressInterface {

    @PostMapping("/postgres/environment/{environment}")
    List<String> checkTenantIdColumn(@PathVariable("environment") String env);

    @GetMapping("/postgres/totalCount/environment/{environment}")
    Map<String, String> getTotalCount(@PathVariable("environment") String env);

    @PostMapping("/postgres/totalCount/environment/{environment}")
    Map<String, Integer> checkTotalRowCount(@PathVariable("environment") String env, @RequestBody List<String> tenantIds);

    @DeleteMapping("/postgres/delete/environment/{environment}")
    Map<String, Integer> deleteTenant(@PathVariable("environment") String env, @RequestBody List<String> tenantIds);
}
