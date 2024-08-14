package com.agilysys.StayTenantPurger.Interfaces;

import com.agilysys.StayTenantPurger.modal.DAO.Tenant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

public interface MainInterface {
    @DeleteMapping("/sync/delete/tenant/environment/{env}")
    public Map<String, Object> startDeletingSync(@RequestBody Tenant tenant, @PathVariable("env") String env);
    @DeleteMapping("/async/delete/tenant/environment/{env}")
    public ResponseEntity<String> startDeletingAsync(@RequestBody Tenant tenant, @PathVariable("env") String env);
}
