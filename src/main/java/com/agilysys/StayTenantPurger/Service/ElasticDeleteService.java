package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.modal.DAO.DeleteQuery;
import com.agilysys.StayTenantPurger.modal.DAO.IndexInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ElasticDeleteService {
    private static final Logger logger = LoggerFactory.getLogger(ElasticDeleteService.class);

    private RestTemplate restTemplate = new RestTemplate();

    public boolean startDeletingTenants(String env, List<String> tenants) {
        List<String> totalIndexes = getIndexes(env).stream().map(IndexInfo::getIndex).toList();
        for (String tenant : tenants) {
            for (String index : totalIndexes) {
                deleteTenant(env, index, tenant);
            }
        }
        return true;
    }

    public List<IndexInfo> getIndexes(String env) {
        String url = getUrl(env) + "/_cat/indices?format=json&bytes=b&s=store.size:desc,index:asc";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        // Use ParameterizedTypeReference to handle the generic type
        ResponseEntity<List<IndexInfo>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<IndexInfo>>() {
        });
        return response.getBody();
    }

    public String deleteDocument(String env, String indexes, String documentId) {
        String url = getUrl(env);
        url = url + "/" + indexes + "/_doc/" + documentId;
        return restTemplate.postForObject(url, null, String.class);
    }

    public String deleteTenant(String env, String indexes, String tenantId) {
        String url = getUrl(env) + "/" + indexes + "/_delete_by_query";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<DeleteQuery> entity = new HttpEntity<>(getQuery(tenantId), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        logger.info(response.getBody());
        return response.getBody().toString();
    }

    public String getUrl(String env) {
        switch (env) {
            case "000":
                return "http://lab-stay-aks-000-server.westus.cloudapp.azure.com:9200";
            case "001":
                return "http://lab-stay-aks-001-server.westus.cloudapp.azure.com:9200";
            case "002":
                return "http://lab-stay-aks-002-server.westus.cloudapp.azure.com:9200";
            case "003":
                return "http://lab-stay-aks-003-server.westus.cloudapp.azure.com:9200";
            case "004":
                return "http://lab-stay-aks-004-server.westus.cloudapp.azure.com:9200";
            case "005":
                return "http://lab-stay-aks-005-server.westus.cloudapp.azure.com:9200";
            case "006":
                return "http://lab-stay-aks-006-server.westus.cloudapp.azure.com:9200";
            case "007":
                return "http://lab-stay-aks-007-server.westus.cloudapp.azure.com:9200";
            default:
                return "";
        }
    }

    public DeleteQuery getQuery(String tenantId) {
        // Construct the Match object
        DeleteQuery.Match match = new DeleteQuery.Match();
        match.setTenantId(tenantId);
        DeleteQuery.Query query = new DeleteQuery.Query();
        query.setMatch(match);
        DeleteQuery deleteQuery = new DeleteQuery();
        deleteQuery.setQuery(query);
        return deleteQuery;
    }
}
