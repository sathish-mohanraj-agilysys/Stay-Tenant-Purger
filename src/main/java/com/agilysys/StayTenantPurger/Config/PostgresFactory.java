package com.agilysys.StayTenantPurger.Config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class PostgresFactory {
    public static synchronized JdbcTemplate getJdbcTemplate(String env) {
        String port = "";
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
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
                dataSource.setUrl("jdbc:postgresql://stay-postgresql-qa.postgres.database.azure.com:5432/aks_stay_qaint");
                dataSource.setUsername("agilysys@stay-postgresql-qa");
                dataSource.setPassword("MhcPykDwpHx2lSIC");
                dataSource.setDriverClassName("org.postgresql.Driver");
                return new JdbcTemplate(dataSource);
            case "qa":
                dataSource.setUrl("jdbc:postgresql://stay-postgresql-qa-05.postgres.database.azure.com:5432/aks_stay_qa");
                dataSource.setUsername("agilysys");
                dataSource.setPassword("MhcPykDwpHx2lSIC");
                dataSource.setDriverClassName("org.postgresql.Driver");
                return new JdbcTemplate(dataSource);
            case "qa03":
                dataSource.setUrl("jdbc:postgresql://stay-postgresql-qa-03.postgres.database.azure.com:5432/postgres");
                dataSource.setUsername("agilysys");
                dataSource.setPassword("a1q@r(b!nuDs2sRUZ36");
                dataSource.setDriverClassName("org.postgresql.Driver");
                return new JdbcTemplate(dataSource);
            case "qa02":
                dataSource.setUrl("jdbc:postgresql://stay-postgresql-qa-02.postgres.database.azure.com:5432/postgres");
                dataSource.setUsername("agilysys");
                dataSource.setPassword("zq%r(n!nuVs2sURZ27");
                dataSource.setDriverClassName("org.postgresql.Driver");
                return new JdbcTemplate(dataSource);
        }
        dataSource.setUrl("jdbc:postgresql://localhost:" + port + "/k3d_localhost");
        dataSource.setUsername("postgres");
        dataSource.setDriverClassName("org.postgresql.Driver");
        return new JdbcTemplate(dataSource);
    }
}
