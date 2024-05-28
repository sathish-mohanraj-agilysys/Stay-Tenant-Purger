package com.agilysys.StayTenantPurger.DAO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Tenant {
    public Set<String> getTenant() {
        return tenant;
    }

    public void setTenant(Set<String> tenant) {
        this.tenant = tenant;
    }

    public Set<String> getProperty() {
        return property;
    }

    public void setProperty(Set<String> property) {
        this.property = property;
    }

    public Set<String> tenant=new HashSet<>();

    @Override
    public String toString() {
        return "Tenant{" +
                "tenant=" + tenant +
                ", property=" + property +
                '}';
    }

    public Set<String> property=new HashSet<>();
}
