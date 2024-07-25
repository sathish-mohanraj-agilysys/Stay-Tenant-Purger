package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.Factory.MongoTemplateFactory;
import com.agilysys.StayTenantPurger.Util.MongoPathFactory;
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
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private MongoPathFactory mongoPathFactory;
    @Autowired
    private MongoTemplateFactory mongoTemplateFactory;

    public Set<String> checkTenants(String env,boolean includeAutomationTenant) {
        Set<String> totalTenants = new HashSet<>();
        Set<String> staleTenants = new HashSet<>();
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
        mongoPathFactory.parallelStream().forEach(x -> {
                    try {
                        if (x.getTenantPath() != null || (x.getTenantPath() == ""))
                            totalTenants.addAll(mongoTemplate.getCollection(x.getName()).distinct(x.getTenantPath(), String.class).into(new HashSet<String>()));
                        else logger.error("No tenant path details or distinct tenants found  for {} collection ", x.getName());
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
        logger.info("Total Tenants found out for the {} environment ->{}", env, totalTenants);
        totalTenants.parallelStream().forEach(tenant -> {
            String url = "https://aks-core-qaint.hospitalityrevolution.com/user-service/tenant/tenants/" + tenant;
            RestTemplate restTemplate = new RestTemplate();
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                System.out.println("name                        ->" + jsonNode.path("name"));

                if (jsonNode.path("name").asText().contains("stayTenant") && includeAutomationTenant) {

                    staleTenants.add(tenant);
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
                if (isValid(tenant)) staleTenants.add(tenant);
            }
        });
        logger.info("Stale tenants found out is" + staleTenants.toString());

        return staleTenants;

    }

    public static boolean isValid(String str) {
        if (str == null) {
            return false;
        }
        return !str.equals("default") && !str.equalsIgnoreCase("Default") && !str.equals("0");
    }
}
