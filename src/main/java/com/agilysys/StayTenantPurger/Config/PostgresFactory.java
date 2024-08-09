package com.agilysys.StayTenantPurger.Config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class PostgresFactory {
    public static synchronized JdbcTemplate getJdbcTemplate(String env) {
        String port = "";
        switch (env) {
            case "000":
                port = String.valueOf(36000);
                break;
            case "001":
                port = String.valueOf(36001);
                break;
            case "002":
                port = String.valueOf(36002);
                break;
            case "003":
                port = String.valueOf(36003);
                break;
            case "004":
                port = String.valueOf(36004);
                break;
            case "005":
                port = String.valueOf(36005);
                break;
            case "006":
                port = String.valueOf(36006);
                break;
            case "007":
                port = String.valueOf(36007);
                break;
            case "008":
                port = String.valueOf(36008);
                break;
            case "009":
                port = String.valueOf(36009);
                break;
            case "qaint":
                DriverManagerDataSource dataSource = new DriverManagerDataSource();
                dataSource.setUrl("jdbc:postgresql://stay-postgresql-qa.postgres.database.azure.com:5432/aks_stay_qaint");
                dataSource.setUsername("agilysys@stay-postgresql-qa");
                dataSource.setPassword("MhcPykDwpHx2lSIC");
                dataSource.setDriverClassName("org.postgresql.Driver");
                return new JdbcTemplate(dataSource);
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:" + port + "/k3d_localhost");
        dataSource.setUsername("postgres");
        dataSource.setDriverClassName("org.postgresql.Driver");
        return new JdbcTemplate(dataSource);
    }
}
