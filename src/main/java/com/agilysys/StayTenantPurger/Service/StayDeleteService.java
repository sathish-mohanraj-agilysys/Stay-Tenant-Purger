package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.Config.DataLoader;
import com.agilysys.StayTenantPurger.DAO.Tenant;
import com.agilysys.StayTenantPurger.Factory.MongoTemplateFactory;
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
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MongoTemplateFactory mongoTemplateFactory;
    @Autowired
    private CoreDeleteSerive coreDeleteSerive;
    @Autowired
    private DataLoader dataLoader;
    private Map<String, Map<String, ArrayList<String>>> yamlMap = dataLoader.readYMlFile();

    private static final Logger logger = LoggerFactory.getLogger(StayDeleteService.class);

    public StayDeleteService() throws FileNotFoundException {
    }


    public String storData(Tenant tenant, String env) {
        File resourceFile = dataLoader.loadCacheFile(env);

        try {

            Tenant temp = objectMapper.readValue(new FileInputStream(resourceFile), Tenant.class);
            temp.getTenant().addAll(tenant.getTenant());
            temp.getProperty().addAll(tenant.getProperty());
            objectMapper.writeValue(resourceFile, temp);
            return temp.toString();
        } catch (IOException e) {
            return "File not found";
        }
    }

    public ResponseEntity deleteInMongodb(String env, boolean isToDeleteCore) {
        Map<String, Integer> deletedOut = new HashMap<>();
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
        File ymlFile = dataLoader.loadYMlFile();
        Yaml yaml = new Yaml();
        Tenant tenantTemp = null;
        try {
            File resourceFile = dataLoader.loadCacheFile(env);
            tenantTemp = objectMapper.readValue(new FileInputStream(resourceFile), Tenant.class);
        } catch (Exception e) {
            System.out.println("cannot initiate the delete operation");
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
                DeleteResult deleteResult = mongoTemplate.remove(query, Object.class, collectionName);
                deletedOut.put(collectionName, (int) deleteResult.getDeletedCount());
                if (deleteResult.getDeletedCount() == 0) logger.info("No doucuments found for the " + collectionName);
                else
                    logger.info(String.format("The %s documents deleted in the %s collection", deleteResult.getDeletedCount(), collectionName));

            });

        } catch (FileNotFoundException e) {
            return new ResponseEntity<>("Cannot not open the file", HttpStatus.FORBIDDEN);
        }
        try {
            File resourceFile = dataLoader.loadCacheFile(env);
            File backUpFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "Backup", env + ".json").toFile();
            Tenant resourceTenant = objectMapper.readValue(new FileInputStream(resourceFile), Tenant.class);
            Tenant backupTenant = objectMapper.readValue(new FileInputStream(backUpFile), Tenant.class);
            backupTenant.getTenant().addAll(resourceTenant.getTenant());
            backupTenant.getProperty().addAll(resourceTenant.getProperty());
            objectMapper.writeValue(backUpFile, backupTenant);
            logger.info("Deleted tenant property is successfully backed up ");
            resourceTenant.getTenant().clear();
            resourceTenant.getProperty().clear();
            objectMapper.writeValue(resourceFile, resourceTenant);
            logger.info("Local Cache is successfully cleaned");
        } catch (Exception e) {
            logger.error("Cannot able to back up the data");
        }

        return new ResponseEntity<>(String.format("The process Success but collection size mismatch found!, %s collections found in configuration file and %s collections found in the %s mongodb /n" + deletedOut.toString(), yamlMap.size(), getAllCollections(env).size(), env), HttpStatus.OK);
    }


    public String clearInLocal(String env) {
        File resourceFile = dataLoader.loadCacheFile(env);
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

    public String getDocumentCountFromCacheDetails(String env) {
        Map<String, Integer> documentCount = new HashMap<>();
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
        Tenant tenantTemp = null;
        try {
            tenantTemp = dataLoader.readDataFromCacheFile(env);
        } catch (Exception e) {
            logger.error("Cannot get the details");
        }
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
            long count = mongoTemplate.count(query, collectionName);
            logger.info(String.format("%s documents present in the %s collection from the cache, in the %s environment", count, collectionName, env));
            documentCount.put(collectionName, (int) count);
        });
        return documentCount.toString();

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
            return "No data has been found in the local cache";
        }

    }

    public Set<String> getAllCollections(String env) {
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
        logger.info("All collections information has been retrieved for the {} environment", env);
        return mongoTemplate.getCollectionNames();
    }

    public Map<String, Integer> getDocumentCount(String env) {
        MongoTemplate mongoTemplate = mongoTemplateFactory.getTemplate(env);
        Map<String, Integer> data = new HashMap<>();
        mongoTemplate.getCollectionNames().stream().parallel().forEach(x -> {
            long count = mongoTemplate.getCollection(x).countDocuments();
            logger.info("The {} documents present in {} collection", count, x);
            data.put(x, (int) count);
        });
        return data;
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