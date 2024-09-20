package com.agilysys.StayTenantPurger.Interfaces;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

public interface EsInterface {
    @GetMapping("/getIndexes/env/{environment}")
    public Object getIndexes(@PathVariable("environment") String env);

    @PostMapping("/deleteTenant/env/{environment}")
    public Map<String, Map<String, String>> deleteTenant(@PathVariable("environment") String env, @RequestBody List<String> tenants);
}
