package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.Config.MongoFactory;
import com.agilysys.StayTenantPurger.Util.MongoPathFactory;
import com.agilysys.StayTenantPurger.Util.Status;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Set;

@Service
public class StaleChecker {
    private static final Logger logger = LoggerFactory.getLogger(StaleChecker.class);
    @Autowired
    private MongoPathFactory mongoPathFactory;
    @Autowired
    private MongoFactory mongoFactory;
    @Autowired
    private ObjectMapper objectMapper;
    private Set<String> totalTenants = new HashSet<>();
    private Set<String> staleTenants = new HashSet<>();

    public Set<String> checkTenants(String env, boolean includeAutomationTenant) {

        MongoTemplate mongoTemplate = mongoFactory.getTemplate(env);
        mongoPathFactory.parallelStream().forEach(x -> {
                    try {
                        if (((x.getTenantPath() != null || (x.getTenantPath() == "")) && !x.getName().equalsIgnoreCase("auditCommits"))) {
                            HashSet<String> tenantIds = mongoTemplate.getCollection(x.getName()).distinct(x.getTenantPath(), String.class).into(new HashSet<String>());
                            totalTenants.addAll(tenantIds);
                        } else
                            logger.error("No tenant path details or distinct tenants found  for {} collection ", x.getName());
                    } catch (Exception e) {
                        logger.error("Error happened {}   ->" + e.getMessage(), x.getName());
                    }
                }
        );


        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "application/json, text/plain, */*");
        headers.add("Api-User-Name", "06d26fd7-8fc9-4e97-ace5-e670bb274aa5");
        headers.add("TENANT_ID", "0");
        headers.add("Client-Id", "662f9b6990acb2221d6061ac");
        headers.add("Nonce", "cfcba8718a0f6f31174dd40e629f5af627139e642ba3d29d6549aec853e79563a96a62eaa78b1a8b8dce412a0a037df641d8cc9fbd6234abd9e1673153b78697");


        HttpEntity<String> entity = new HttpEntity<>(headers);
        String totalTenant;
        try {
            totalTenant = objectMapper.writeValueAsString(totalTenants);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.debug("Total Tenants found out for the {} environment ->{}", env, totalTenant);
        totalTenants.parallelStream().forEach(tenant -> {
            String url = "https://aks-core-qaint.hospitalityrevolution.com/user-service/tenant/tenants/" + tenant;
            RestTemplate restTemplate = new RestTemplate();
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                logger.debug("[{}] -> TenantId:{}; TenantName:{}", Status.RESULT, tenant, jsonNode.path("name"));
                if (jsonNode.path("name").asText().contains("stayTenant") && includeAutomationTenant) {
                    this.addTenant(tenant);
                }

            } catch (Exception e) {
                logger.debug("NOT FOUND: {} tenantId in the core [ ERROR: {}]", tenant, e.getMessage());
                if (isValid(tenant)) {
                    this.addTenant(tenant);
                }
            }
        });
        try {
            logger.info("Stale tenants found out is {}" ,  objectMapper.writeValueAsString(staleTenants));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return staleTenants;

    }

    public static boolean isValid(String str) {
        if (str == null) {
            return false;
        }
        boolean isvalid = !str.equals("default") && !str.equalsIgnoreCase("Default") && !str.equals("0") && !str.equals("null");
        if (!isvalid) {
            logger.info("[{}] Tenant from the stale count: {}", Status.SKIPPING, str);
        }
        return isvalid;
    }

    private void addTenant(String tenant) {
        try {
            if (Integer.parseInt(tenant) > 0 && Integer.parseInt(tenant) < 700000) {
                staleTenants.add(tenant);
            }
        } catch (Exception e) {
            logger.error("Tenant parsing failed for {} with error {}" ,tenant, e.getMessage());
            System.out.println();
        }
    }
}
