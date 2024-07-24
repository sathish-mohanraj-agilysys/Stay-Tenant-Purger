package com.agilysys.StayTenantPurger.Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

public class TenantFinder {

    public static void main(String[] args) throws JsonProcessingException {
        String[] tenantList = {"100", "1021", "1023", "1058", "1061", "109", "110", "1132", "1167", "1188", "1212", "1218", "122", "1236", "126582", "129307", "1318", "1336", "1337", "1339", "1344", "1439", "147987", "1480", "1487", "1532", "1548", "1551", "1566", "1568", "1586", "1595", "1598", "1613", "1614", "161630", "1673", "1676", "171", "1738", "1791", "1794", "180268", "1805", "1815", "1816", "1832", "1838", "1841", "1845", "1852", "1854", "1861", "1864", "1865", "1891", "1906", "1907", "1910", "1911", "1917", "1919", "1920", "1930", "193469", "1936", "1937", "1955", "1956", "1958", "1961", "1966", "1990", "2004", "200840", "2025", "2051", "2062", "2065", "2068", "2069", "2078", "208725", "2090", "2091", "2093", "2094", "2117", "214477", "2145", "2190", "220210", "224373", "227143", "227516", "230151", "248344", "253", "264101", "277472", "283643", "290813", "292236", "332129", "353521", "375714", "383983", "387501", "397824", "397915", "398213", "399623", "406372", "406955", "408674", "410", "416089", "417961", "418141", "418142", "422463", "425243", "425842", "460", "494", "518", "529", "592", "648", "686", "688", "734", "871", "874", "884", "901", "930", "933", "936", "939", "940", "94194", "950"};
        ArrayList<String> tenant = new ArrayList<>();
        for (int i = 0; i < tenantList.length; i++) {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://aks-core-qaint.hospitalityrevolution.com/user-service/tenant/tenants/" + tenantList[i];

            HttpHeaders headers = new HttpHeaders();
            headers.add("accept", "application/json, text/plain, */*");
            headers.add("Api-User-Name", "06d26fd7-8fc9-4e97-ace5-e670bb274aa5");
            headers.add("TENANT_ID", "0");
            headers.add("Client-Id", "662f9b6990acb2221d6061ac");
            headers.add("Nonce", "cfcba8718a0f6f31174dd40e629f5af627139e642ba3d29d6549aec853e79563a96a62eaa78b1a8b8dce412a0a037df641d8cc9fbd6234abd9e1673153b78697");


            HttpEntity<String> entity = new HttpEntity<>(headers);

            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                System.out.println("running");
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                System.out.println("name                        ->" + jsonNode.path("name"));
//               if (jsonNode.path("name").asText().contains("stayTenant")) {
//
//                   tenant.add(tenantList[i]);
//               }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                tenant.add(tenantList[i]);
            }


        }
        System.out.println(tenant.toString());

    }
}
