package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.Util.Status;
import com.agilysys.StayTenantPurger.modal.DAO.DeleteQuery;
import com.agilysys.StayTenantPurger.modal.DAO.ElasticsearchResponse;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.agilysys.StayTenantPurger.Util.Base64Generator.generateBasicAuth;

@Service
public class ElasticDeleteService {
    private static final Logger logger = LoggerFactory.getLogger(ElasticDeleteService.class);

    private RestTemplate restTemplate = new RestTemplate();

    public Map<String, Map<String ,String>> startDeletingTenants(String env, List<String> tenants) {
        logger.info("[{}] Deleting the {} tenants in the Elastic Search", Status.STARTED, tenants);
        List<String> totalIndexes = getIndexes(env).stream().map(IndexInfo::getIndex).toList();
        if (Stream.of("001", "002", "003", "004", "005", "006", "007", "008", "009", "ui").noneMatch((x -> x.equalsIgnoreCase(env)))) {
            totalIndexes = totalIndexes.stream().filter(x -> x.contains("aks-stay-" + env + "_")).toList();
        }
        logger.info("[{}] ES Indexes {}  in {} Elastic Search", Status.FOUND, totalIndexes.toString(),env);
        List<String> finalTotalIndexes = totalIndexes;
        Map<String,Map<String ,String>> esDeletedData=new ConcurrentHashMap<>();
        tenants.parallelStream().forEach(tenant -> {
            Map<String ,String> singleTenantESData=new ConcurrentHashMap<>();
            finalTotalIndexes.parallelStream().forEach(index -> {
                String esDeletedCount = String.valueOf(deleteTenant(env, index, tenant));
                singleTenantESData.put(index,esDeletedCount);
                logger.debug("[{}] Deleting tenant from the ES for index {} is of count {}", tenant, index, esDeletedCount);
            });
            esDeletedData.put(tenant,singleTenantESData);
        });
        logger.debug("ELASTIC_SEARCH_DELETED_OUTPUT {}",esDeletedData);
        return esDeletedData;
    }

    public List<IndexInfo> getIndexes(String env) {
        String url = getUrl(env) + "/_cat/indices?format=json&bytes=b&s=store.size:desc,index:asc";
        HttpEntity<String> entity = new HttpEntity<>(getHeader(env));
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

    public int deleteTenant(String env, String indexes, String tenantId) {
        String url = getUrl(env) + "/" + indexes + "/_delete_by_query";
        HttpEntity<DeleteQuery> entity = new HttpEntity<>(getQuery(tenantId), getHeader(env));
        ResponseEntity<ElasticsearchResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, ElasticsearchResponse.class);
        logger.debug("[{}] ES deleted count for tenant {} and indexes {} is {}", Status.COMPLETED, tenantId, indexes, Objects.requireNonNull(response.getBody()).getDeleted());
        return response.getBody().getDeleted();
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
            case "qa02":
                return "stay-elasticsearch-qa-02.es.westus2.azure.elastic-cloud.com";
            case "qa03":
                return "stay-elasticsearch-qa-03.es.westus2.azure.elastic-cloud.com";
            case "qaint", "qa":
                return "https://78487f86271b4a9b90ba28a848826f45.westus2.azure.elastic-cloud.com";
            default:
                return "";
        }
    }

    public HttpHeaders getHeader(String env) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        switch (env) {
            case "qaint","qa":
                headers.set("Authorization", generateBasicAuth("stay-services", "flu1skew*dorn2KROX"));
                return headers;
            case "qa02":
                headers.set("Authorization", generateBasicAuth("stay-services", "kNGdnZ7iNT7LnQJ"));
            case "qa03":
                headers.set("Authorization", generateBasicAuth("stay-services", "JvaZz7z898QeHjGk"));
                return headers;
            default:
                return headers;
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
