package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.DAO.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.DeleteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

@Service
public class StayDeleteService {
    @Autowired
    ResourceLoader resourceLoader;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MongoTemplate mongoTemplate;


    private static final Logger logger = LoggerFactory.getLogger(StayDeleteService.class);


    public String storData(Tenant tenant, String env) {
        File resourceFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", env + ".json").toFile();

        try {

            Tenant temp = objectMapper.readValue(new FileInputStream(resourceFile), Tenant.class);
            temp.getTenant().addAll(tenant.getTenant());
            temp.getProperty().addAll(tenant.getProperty());
            objectMapper.writeValue(resourceFile, temp);
            logger.info("Tenant: {}", temp.toString());
            logger.info("The environment is: {}", env);
            logger.info(temp.toString());
            return temp.toString();
        } catch (IOException e) {
            return "File not found";
        }
    }

    public ResponseEntity deleteInMongodb(String env) {
        File ymlFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "rGuestStaymap.yml").toFile();
        Yaml yaml = new Yaml();

        try {
            Map<String, Map<String, ArrayList<String>>> yamlMap = yaml.load(new FileInputStream(ymlFile));
            yamlMap.entrySet().stream().parallel().forEach(entry -> {
                String collectionName = entry.getKey();
                String tenantPath = "";
                String propertyPath = "";
                if (entry.getValue().get("tenantId") != null) {
                    tenantPath = entry.getValue().get("tenantId").stream().reduce((s1, s2) -> s1 + "." + s2).orElse("");
                }
                if (entry.getValue().get("propertyId") != null) {
                    propertyPath = entry.getValue().get("propertyId").stream().reduce((s1, s2) -> s1 + "." + s2).orElse("");
                }
                File resourceFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", env + ".json").toFile();
                Tenant tenantTemp = null;
                try {

                    tenantTemp = objectMapper.readValue(new FileInputStream(resourceFile), Tenant.class);
                } catch (Exception e) {
                    System.out.println("cannot initiate the delete operation");
                }
                Criteria criteria;
                if (!tenantPath.equalsIgnoreCase("") && !propertyPath.equalsIgnoreCase("")) {
                    criteria = new Criteria().orOperator(
                            Criteria.where(tenantPath).in(tenantTemp.getTenant()),
                            Criteria.where(propertyPath).in(tenantTemp.getProperty())
                    );
                } else if (tenantPath.equalsIgnoreCase("") && !propertyPath.equalsIgnoreCase("")) {
                    criteria = new Criteria().orOperator(

                            Criteria.where(propertyPath).in(tenantTemp.getProperty())
                    );
                } else {
                    criteria = new Criteria().orOperator(
                            Criteria.where(tenantPath).in(tenantTemp.getTenant()));
                }

                Query query = new Query(criteria);
                DeleteResult deleteResult = mongoTemplate.remove(query, Object.class, collectionName);
                if (deleteResult.getDeletedCount() == 0) logger.info("No doucuments found for the " + collectionName);
                else
                    logger.info(String.format("The %s documents deleted in the %s collection", deleteResult.getDeletedCount(), collectionName));

            });

        } catch (FileNotFoundException e) {
            return new ResponseEntity<>("Cannot not open the file", HttpStatus.FORBIDDEN);
        }
        try {
            File resourceFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", env + ".json").toFile();
            File backUpFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "Backup", env + ".json").toFile();
            Tenant resourceTenant = objectMapper.readValue(new FileInputStream(resourceFile), Tenant.class);
            Tenant backupTenant = objectMapper.readValue(new FileInputStream(backUpFile), Tenant.class);
            backupTenant.getTenant().addAll(resourceTenant.getTenant());
            backupTenant.getProperty().addAll(resourceTenant.getProperty());
            objectMapper.writeValue(backUpFile, backupTenant);
            logger.info("Deleted tenant property is successfully backed up ");
        } catch (Exception e) {
            logger.error("Cannot able to back up the data");
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }


    public String clearInLocal(String env) {
        File resourceFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", env + ".json").toFile();
        try {
            Tenant temp = objectMapper.readValue(new FileInputStream(resourceFile), Tenant.class);
            temp.getProperty().clear();
            temp.getTenant().clear();
            objectMapper.writeValue(resourceFile, temp);
            logger.info("The tenant and property data in the local cache has been cleared for the " + env + "environment");

            logger.info(temp.toString());
            return temp.toString();
        } catch (Exception e) {
            return "Cannot open the source file";
        }

    }


    public String dropAllCollections(String env) {
        for (String collection : mongoTemplate.getCollectionNames()) {
            mongoTemplate.dropCollection(collection);
            logger.info(String.format("Dropped the %s collection", collection));
        }
        return "Successfully dropped";
    }

    public String getDataFromCache(String env) {
        File resourceFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", env + ".json").toFile();

        try {
            Tenant temp = objectMapper.readValue(new FileInputStream(resourceFile), Tenant.class);
            logger.info(String.format("Data returned from the cache for the %s environment is %s ",env,temp.toString()));
            return temp.toString();
        } catch (IOException e) {
            return "No data has been found in the local cache";
        }

    }

    public Set<String> getAllCollections(String env) {
        logger.info("All collections information has been retrieved for the {} environment",env);
        return mongoTemplate.getCollectionNames();
    }
}
