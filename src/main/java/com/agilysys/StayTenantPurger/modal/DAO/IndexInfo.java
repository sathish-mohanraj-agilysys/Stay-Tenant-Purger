package com.agilysys.StayTenantPurger.modal.DAO;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class IndexInfo {
    private String health;
    private String status;
    private String index;
    private String uuid;
    private String pri;
    private String rep;
    private String docsCount;
    private String docsDeleted;
    private String storeSize;
    private String priStoreSize;
}
