package com.agilysys.StayTenantPurger.modal.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MongoConnection {
    private String envName;
    private String url;
    private String dbName;
}
