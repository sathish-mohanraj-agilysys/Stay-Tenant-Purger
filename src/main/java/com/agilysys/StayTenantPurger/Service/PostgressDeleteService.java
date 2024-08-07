package com.agilysys.StayTenantPurger.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.agilysys.StayTenantPurger.Config.PostgresFactory.getJdbcTemplate;

@Service
public class PostgressDeleteService {
    private static final Logger logger = LoggerFactory.getLogger(PostgressDeleteService.class);

    public List<String> checkForTenantId(String env) {
        return checkTablesForTenantId(getJdbcTemplate(env));
    }

    public Map<String, Integer> countDocuments(String env, List<String> tenantIds) {
        List<String> tableNames = checkForTenantId(env);
        Set<String> remainingTask = new ConcurrentSkipListSet<>(tableNames);
        logger.info("{} Tables found in the {} environment", tableNames.size(), env);
        if (tenantIds.isEmpty()) {
            return null;
        }
        // Convert tenantIds to a comma-separated string
        String tenantIdList = tenantIds.stream()
                .map(id -> "'" + id + "'")
                .collect(Collectors.joining(","));

        AtomicInteger totalDocumentCount = new AtomicInteger();
        Map<String, Integer> tableCount = new ConcurrentHashMap<>();

        tableNames.parallelStream().forEach(tableName -> {
            JdbcTemplate jdbcTemplate = getJdbcTemplate(env);
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE tenant_id IN (" + tenantIdList + ")";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            totalDocumentCount.addAndGet(count != null ? count : 0);
            tableCount.put(tableName, count);
            remainingTask.remove(tableName);
            System.out.println("Found " + count + " rows in table " + tableName);
            logger.info(remainingTask.toString());
        });

        return tableCount;
    }


    public Map<String, Integer> deleteTenant(String env, List<String> tenantIds) {
        List<String> tableNames = checkForTenantId(env);

        if (tenantIds.isEmpty()) {
            return null;
        }

        // Convert tenantIds to a comma-separated string
        String tenantIdList = tenantIds.parallelStream()
                .map(id -> "'" + id + "'")
                .collect(Collectors.joining(","));

        Map<String, Integer> tableCount = new ConcurrentHashMap<>();
        tableNames.parallelStream().forEach(tableName -> {
            logger.info("Deleting on the table " + tableName);
            JdbcTemplate jdbcTemplate = getJdbcTemplate(env);
            String sql = "DELETE FROM " + tableName + " WHERE tenant_id IN (" + tenantIdList + ")";
            int deletedCount = jdbcTemplate.update(sql);
            tableCount.put(tableName, deletedCount);
            logger.info("Deleted " + deletedCount + " rows from table " + tableName);
        });
        return tableCount;
    }

    public List<String> checkTablesForTenantId(JdbcTemplate jdbcTemplate) {
        // Query to get all table names in the public schema
        String tableQuery = "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'";
        List<String> tableNames = jdbcTemplate.queryForList(tableQuery, String.class);

        // Check if each table contains the tenant_id column
        List<String> tablesWithoutTenantId = tableNames.parallelStream()
                .filter(table -> hasTenantIdColumn(jdbcTemplate, table))
                .toList();

        // Print results
        if (tablesWithoutTenantId.isEmpty()) {
            System.out.println("All tables contain the tenant_id column.");
        } else {
            System.out.println("The following tables do not contain the tenant_id column:");
            tablesWithoutTenantId.forEach(System.out::println);
        }
        return tablesWithoutTenantId;
    }

    private boolean hasTenantIdColumn(JdbcTemplate jdbcTemplate, String tableName) {
        String columnQuery = "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = ? AND column_name = 'tenant_id'";
        Integer count = jdbcTemplate.queryForObject(columnQuery, new Object[]{tableName}, Integer.class);
        return count != null && count > 0;
    }


}
