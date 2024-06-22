package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.Config.DataLoader;
import com.agilysys.StayTenantPurger.DAO.Tenant;
import com.agilysys.StayTenantPurger.Factory.MongoTemplateFactory;
import com.agilysys.StayTenantPurger.Util.MongoPathFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBObject;
import com.mongodb.client.result.DeleteResult;
import org.bson.BsonBinaryWriter;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DocumentCodec;
import org.bson.io.BasicOutputBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StayDeleteService {
    private static final Logger logger = LoggerFactory.getLogger(StayDeleteService.class);
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MongoTemplateFactory mongoTemplateFactory;
    @Autowired
    private CoreDeleteSerive coreDeleteSerive;
    @Autowired
    private DataLoader dataLoader;
    @Autowired
    private MongoPathFactory mongoPathFactory;
    public String storData(Tenant tenant, String env) {
        try {
            Tenant temp = dataLoader.readDataFromCacheFile(env);
            temp.getTenant().addAll(tenant.getTenant());
            temp.getProperty().addAll(tenant.getProperty());
            dataLoader.writeDataIntoCacheFile(env, temp);
            return temp.toString();
        } catch (IOException e) {
            return String.format("Caching not found for the %s environment: %s", env, e.getMessage());
        }
    }

    public ResponseEntity deleteInMongodb(String env, boolean isToDeleteCore) {
        Map<String, Integer> deletedOut = new HashMap<>();
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
        Tenant tenantToDelete = null;
        Map<String, Map<String, ArrayList<String>>> yamlMap;

        try {
            tenantToDelete =dataLoader.readDataFromCacheFile(env);
            yamlMap = dataLoader.readYMlFile();
            if (yamlMap.size() != getAllCollections(env).size()) {
                logger.error("The collection size mismatch found!, {} collections found in configuration file and {} collections found in the mongodb", yamlMap.size(), getAllCollections(env).size());
            }
            Tenant finalTenantTemp = tenantToDelete;
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
                Criteria criteria = null;
                if (entry.getKey().equalsIgnoreCase("config") || entry.getKey().equalsIgnoreCase("configEvents")) {
                    assert finalTenantTemp != null;
                    Set<String> tenantAndProperty = finalTenantTemp.getProperty();
                    tenantAndProperty.addAll(finalTenantTemp.getTenant());
                    if (tenantAndProperty.size() == 0) {
                        logger.info("No doucuments found for the " + collectionName);
                        return;
                    }
                    String regex = tenantAndProperty.stream().collect(Collectors.joining("|"));
                    criteria = Criteria.where("path").regex(regex);

                } else if (!tenantPath.equalsIgnoreCase("") && !propertyPath.equalsIgnoreCase("")) {
                    criteria = new Criteria().orOperator(
                            Criteria.where(tenantPath).in(finalTenantTemp.getTenant()),
                            Criteria.where(propertyPath).in(finalTenantTemp.getProperty())
                    );
                } else if (tenantPath.equalsIgnoreCase("") && !propertyPath.equalsIgnoreCase("")) {
                    criteria = new Criteria().orOperator(

                            Criteria.where(propertyPath).in(finalTenantTemp.getProperty())
                    );
                } else {
                    criteria = new Criteria().orOperator(
                            Criteria.where(tenantPath).in(finalTenantTemp.getTenant()));
                }

                Query query = new Query(criteria);
                DeleteResult deleteResult = mongoTemplate.remove(query, Object.class, collectionName);
                deletedOut.put(collectionName, (int) deleteResult.getDeletedCount());
                if (deleteResult.getDeletedCount() == 0) logger.info("No doucuments found for the " + collectionName);
                else
                    logger.info(String.format("The %s documents deleted in the %s collection", deleteResult.getDeletedCount(), collectionName));

            });

        } catch (FileNotFoundException e) {
            return new ResponseEntity<>("Cannot not open the file", HttpStatus.FORBIDDEN);
        } catch (IOException e) {
            return new ResponseEntity<>("Cannot able to get the cache data", HttpStatus.FORBIDDEN);
        }
        try {
            Tenant backupTenant = dataLoader.readDataFromBackupFile(env);
            backupTenant.getTenant().addAll(tenantToDelete.getTenant());
            backupTenant.getProperty().addAll(tenantToDelete.getProperty());
            dataLoader.writeDataIntoBackupFile(env,backupTenant);
            logger.info("Deleted details is successfully backed up for the {} environment",env);
            dataLoader.writeDataIntoCacheFile(env,new Tenant());
            logger.info("Local Cache is successfully cleaned for {} environment",env);
        } catch (Exception e) {
            logger.error("Error in backup for {} environment",env);
        }

        return new ResponseEntity<>(String.format("The process Success but collection size mismatch found!, %s collections found in configuration file and %s collections found in the %s mongodb /n" + deletedOut.toString(), yamlMap.size(), getAllCollections(env).size(), env), HttpStatus.OK);
    }


    public String clearInLocal(String env) {
        try {
            dataLoader.writeDataIntoCacheFile(env, new Tenant());
            logger.info("The tenant and property data in the local cache has been cleared for the " + env + "environment");
            return dataLoader.readDataFromCacheFile(env).toString();
        } catch (Exception e) {
            return String.format("Error in clearing the cache for the %s environment ", env) + e;
        }

    }

    public String getDocumentCountFromCacheDetails(String env) {
        Map<String, Integer> documentCount = new HashMap<>();
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
        File ymlFile = dataLoader.loadYMlFile();
        Yaml yaml = new Yaml();
        Tenant tenantTemp = null;
        try {
            File resourceFile = dataLoader.loadCacheFile(env);
            tenantTemp = objectMapper.readValue(new FileInputStream(resourceFile), Tenant.class);
        } catch (Exception e) {
            logger.error("Cannot get the details");
        }
        Map<String, Map<String, ArrayList<String>>> yamlMap;
        try {
            yamlMap = yaml.load(new FileInputStream(ymlFile));
            if (yamlMap.size() != getAllCollections(env).size()) {
                logger.error("The collection size mismatch found!, {} collections found in configuration file and {} collections found in the mongodb", yamlMap.size(), getAllCollections(env).size());
            }
            Tenant finalTenantTemp = tenantTemp;
            mongoPathFactory.stream().parallel().forEach(mongoCollection -> {

                Criteria criteria = null;
                if (mongoCollection.getName().equalsIgnoreCase("config") || mongoCollection.getName().equalsIgnoreCase("configEvents")) {
                    assert finalTenantTemp != null;
                    Set<String> tenantAndProperty = finalTenantTemp.getProperty();
                    tenantAndProperty.addAll(finalTenantTemp.getTenant());
                    if (tenantAndProperty.size() == 0) {
                        logger.info("No doucuments found for the " + mongoCollection.getName());
                        return;
                    }
                    String regex = tenantAndProperty.stream().collect(Collectors.joining("|"));
                    criteria = Criteria.where("path").regex(regex);

                } else if (!mongoCollection.getTenantPath().equalsIgnoreCase("") && !mongoCollection.getPropertyPath().equalsIgnoreCase("")) {
                    criteria = new Criteria().orOperator(
                            Criteria.where(mongoCollection.getTenantPath()).in(finalTenantTemp.getTenant()),
                            Criteria.where(mongoCollection.getPropertyPath()).in(finalTenantTemp.getProperty())
                    );
                } else if (mongoCollection.getTenantPath().equalsIgnoreCase("") && !mongoCollection.getPropertyPath().equalsIgnoreCase("")) {
                    criteria = new Criteria().orOperator(

                            Criteria.where(mongoCollection.getPropertyPath()).in(finalTenantTemp.getProperty())
                    );
                } else {
                    criteria = new Criteria().orOperator(
                            Criteria.where(mongoCollection.getTenantPath()).in(finalTenantTemp.getTenant()));
                }

                Query query = new Query(criteria);
                long count = mongoTemplate.count(query, mongoCollection.getName());
                logger.info(String.format("%s documents present in the %s collection from the cache, in the %s environment", count, mongoCollection.getName(), env));
                documentCount.put(mongoCollection.getName(), (int) count);
            });
            return documentCount.toString();

        } catch (FileNotFoundException e) {
            return "Cannot make operation";
        }
    }

    public String getDocumentCountFromCacheDetailsAndBackup(String env) {
        try {
            Files.createDirectories(Paths.get(System.getProperty("user.dir"), "Cloned"));
        } catch (IOException e) {
            logger.error(e.toString());
        }
        Map<String, Integer> documentCount = new HashMap<>();
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
        File ymlFile = dataLoader.loadCacheFile(env);
        Yaml yaml = new Yaml();
        Tenant tenantTemp = null;
        try {
            File resourceFile = dataLoader.loadCacheFile(env);
            tenantTemp = objectMapper.readValue(new FileInputStream(resourceFile), Tenant.class);
        } catch (Exception e) {
            logger.error("Cannot get the details");
        }
        Map<String, Map<String, ArrayList<String>>> yamlMap;
        try {
            yamlMap = yaml.load(new FileInputStream(ymlFile));
            if (yamlMap.size() != getAllCollections(env).size()) {
                logger.error("The collection size mismatch found!, {} collections found in configuration file and {} collections found in the mongodb", yamlMap.size(), getAllCollections(env).size());
            }
            Tenant finalTenantTemp = tenantTemp;
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
                Criteria criteria = null;
                if (entry.getKey().equalsIgnoreCase("config") || entry.getKey().equalsIgnoreCase("configEvents")) {
                    assert finalTenantTemp != null;
                    Set<String> tenantAndProperty = finalTenantTemp.getProperty();
                    tenantAndProperty.addAll(finalTenantTemp.getTenant());
                    if (tenantAndProperty.size() == 0) {
                        logger.info("No doucuments found for the " + collectionName);
                        return;
                    }
                    String regex = tenantAndProperty.stream().collect(Collectors.joining("|"));
                    criteria = Criteria.where("path").regex(regex);

                } else if (!tenantPath.equalsIgnoreCase("") && !propertyPath.equalsIgnoreCase("")) {
                    criteria = new Criteria().orOperator(
                            Criteria.where(tenantPath).in(finalTenantTemp.getTenant()),
                            Criteria.where(propertyPath).in(finalTenantTemp.getProperty())
                    );
                } else if (tenantPath.equalsIgnoreCase("") && !propertyPath.equalsIgnoreCase("")) {
                    criteria = new Criteria().orOperator(

                            Criteria.where(propertyPath).in(finalTenantTemp.getProperty())
                    );
                } else {
                    criteria = new Criteria().orOperator(
                            Criteria.where(tenantPath).in(finalTenantTemp.getTenant()));
                }

                Query query = new Query(criteria);
                List<DBObject> documents = mongoTemplate.find(query, DBObject.class, collectionName);

                writeInBson(Paths.get(System.getProperty("user.dir"), "Cloned", collectionName + ".bson"), documents);
                logger.info(String.format(collectionName + " completed"));
            });
            return documentCount.toString();

        } catch (FileNotFoundException e) {
            return "Cannot make operation";
        }
    }


    public String dropAllCollections(String env) {
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
        for (String collection : mongoTemplate.getCollectionNames()) {
            mongoTemplate.dropCollection(collection);
            logger.info(String.format("Dropped the %s collection", collection));
        }
        return "Successfully dropped";
    }

    public String getDataFromCache(String env) {
          try {
            Tenant temp = dataLoader.readDataFromCacheFile(env);
            logger.info(String.format("Data returned from the cache for the %s environment is %s ", env, temp.toString()));
            return temp.toString();
        } catch (IOException e) {
            return String.format("Error in Getting the cache file for %s environment ",env)+e;
        }

    }

    public Set<String> getAllCollections(String env) {
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
        logger.info("All collections information has been retrieved for the {} environment", env);
        return mongoTemplate.getCollectionNames();
    }

    public Map<String, Integer> getDocumentCount(String env) {
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
       return mongoTemplate.getCollectionNames().parallelStream()
                .collect(Collectors.toMap(
                        collectionName -> collectionName,
                        collectionName -> {
                            long count = mongoTemplate.getCollection(collectionName).countDocuments();
                            logger.info("{} documents are in {} collection in {}", count, collectionName,env);
                            return (int) count;
                        }
                ));
    }

    public synchronized void writeInBson(Path filePath, List<DBObject> documents) {
        File backupFile = filePath.toFile();
        if (!backupFile.exists()) {
            try {
                backupFile.createNewFile();
            } catch (IOException e) {
                logger.error(e.toString());
            }
        }
        try (FileOutputStream fos = new FileOutputStream(backupFile)) {
            for (DBObject document : documents) {
                BasicOutputBuffer buffer = new BasicOutputBuffer();
                BsonWriter writer = new BsonBinaryWriter(buffer);

                // Convert DBObject to Document
                Document doc = new Document(document.toMap());

                // Use DocumentCodec to encode Document to BsonWriter
                new DocumentCodec().encode(writer, doc, org.bson.codecs.EncoderContext.builder().isEncodingCollectibleDocument(true).build());

                writer.flush();
                fos.write(buffer.toByteArray());
            }
        } catch (Exception e) {
            logger.error(e.toString());
        }

    }
}