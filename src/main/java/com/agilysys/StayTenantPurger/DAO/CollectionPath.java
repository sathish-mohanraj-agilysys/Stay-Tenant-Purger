package com.agilysys.StayTenantPurger.DAO;

public class CollectionPath {
    private String name;
    private String tenantPath;
    private String propertyPath;


    public CollectionPath(String name, String tenantPath, String propertyPath) {
        this.name = name;
        this.tenantPath = tenantPath;
        this.propertyPath = propertyPath;
    }

    public String getTenantPath() {
        return tenantPath;
    }

    public void setTenantPath(String tenantPath) {
        this.tenantPath = tenantPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public void setPropertyPath(String propertyPath) {
        this.propertyPath = propertyPath;
    }


}
