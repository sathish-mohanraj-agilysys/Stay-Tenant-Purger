package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.Config.DataLoader;
import com.agilysys.StayTenantPurger.Config.MongoFactory;
import com.agilysys.StayTenantPurger.Util.MongoPathFactory;
import com.agilysys.StayTenantPurger.modal.DAO.CollectionPath;
import com.agilysys.StayTenantPurger.modal.DAO.Tenant;
import com.mongodb.client.MongoCursor;
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
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class StayMongoDeleteService {
    private final int BATCH_SIZE = 30000;
    private final Path CLONING_PATH = Paths.get(System.getProperty("user.dir"), "Cloned");
    private static final Logger logger = LoggerFactory.getLogger(StayMongoDeleteService.class);
    @Autowired
    MongoFactory mongoFactory;
    @Autowired
    private CoreDeleteSerive coreDeleteSerive;
    @Autowired
    private DataLoader dataLoader;
    @Autowired
    private MongoPathFactory mongoPathFactory;

    public String storeData(Tenant tenant, String env) {
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

    public ResponseEntity<Map<String, Integer>> deleteInMongodb(String env, boolean isToDeleteCore) {
        Set<String> collections = getAllCollections(env);
        Map<String, Integer> deletedOut;
        MongoTemplate mongoTemplate =mongoFactory.getTemplate(env);
        Tenant tenantToDelete;

        try {
            tenantToDelete = dataLoader.readDataFromCacheFile(env);
            Set<String> taskRemaining = ConcurrentHashMap.newKeySet();
             taskRemaining.addAll( mongoPathFactory.stream().map(CollectionPath::getName).collect(Collectors.toSet()));
            if (mongoPathFactory.size() != collections.size()) {
                logger.error("The collection size mismatch found!, {} collections found in configuration file and {} collections found in the mongodb", mongoPathFactory.size(), collections.size());
            }
            Tenant finalTenantTemp = tenantToDelete;
            ExecutorService executorService = Executors.newFixedThreadPool(100);
            Map<String, Future<Integer>> futureResults = new HashMap<>();

            mongoPathFactory.forEach(mongoCollection -> {
                Callable<Integer> task = () -> {
                    boolean isPresent = true;
                    int deletedCount = 0;
                    Query query = mongoPathFactory.querryBuilder.build(mongoCollection, finalTenantTemp);
                    query.limit(BATCH_SIZE);
                    query.fields().include("_id");
                    int count =0;
                    while (isPresent) {
                        count++;
                        logger.info("Going to start {} batch querying in  {} collection" ,count,mongoCollection.getName());
                        List<Document> documents = mongoTemplate.find(query, Document.class, mongoCollection.getName());
                        logger.info("Got the output {} batch in  {} collection" ,count,mongoCollection.getName());
                        if (!documents.isEmpty()) {
                            Criteria batchCriteria = Criteria.where("_id").in(documents.stream().map(x -> x.get("_id")).collect(Collectors.toSet()));
                            logger.info("Going to start {} batch deleting in {} collection" ,count,mongoCollection.getName());
                            DeleteResult deleteResult = mongoTemplate.remove(new Query(batchCriteria), Object.class, mongoCollection.getName());
                            logger.info("Deleting completed for {} batch  in {} collection" ,count,mongoCollection.getName());
                            deletedCount += (int) deleteResult.getDeletedCount();
                        } else {
                            isPresent = false;
                        }
                    }
                    taskRemaining.remove(mongoCollection.getName());
                    if (deletedCount == 0) {
                        logger.info("No documents found for the {}", mongoCollection.getName());
                    } else {
                        logger.info("The {} documents deleted in the {} collection", deletedCount, mongoCollection.getName());
                    }
                    logger.info("Remaining collections are {}", taskRemaining);
                    return deletedCount;
                };
                futureResults.put(mongoCollection.getName(), executorService.submit(task));
            });

            deletedOut = new HashMap<>();
            for (Map.Entry<String, Future<Integer>> entry : futureResults.entrySet()) {
                try {
                    deletedOut.put(entry.getKey(), entry.getValue().get());
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error processing collection {}: {}", entry.getKey(), e.getMessage());
                }
            }

            executorService.shutdown();

        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        } catch (IOException e) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }

        try {
            Tenant backupTenant = dataLoader.readDataFromBackupFile(env);
            backupTenant.getTenant().addAll(tenantToDelete.getTenant());
            backupTenant.getProperty().addAll(tenantToDelete.getProperty());
            dataLoader.writeDataIntoBackupFile(env, backupTenant);
            logger.info("Deleted details are successfully backed up for the {} environment", env);
            dataLoader.writeDataIntoCacheFile(env, new Tenant());
            logger.info("Local Cache is successfully cleaned for {} environment", env);
        } catch (Exception e) {
            logger.error("Error in backup for {} environment", env);
        }

        return new ResponseEntity<>( deletedOut, HttpStatus.OK);
    }

    public String clearInLocal(String env) {
        try {
            dataLoader.writeDataIntoCacheFile(env, new Tenant());
            logger.info("The tenant and property data in the local cache has been cleared for the {} environment",env);
            return dataLoader.readDataFromCacheFile(env).toString();
        } catch (Exception e) {
            return String.format("Error in clearing the cache for the %s environment ", env) + e;
        }

    }

    public Map<String, Long> getDocumentCountFromCacheDetails(String env) {
        MongoTemplate mongoTemplate =mongoFactory.getTemplate(env);
        Set<String> collections = getAllCollections(env);
        Tenant tenantTemp = null;
        try {
            tenantTemp = dataLoader.readDataFromCacheFile(env);
        } catch (Exception e) {
            logger.error("Cannot able to load Cache for {} environment", env);
        }
        if (mongoPathFactory.size() != collections.size()) {
            logger.warn("The collection size mismatch found!, {} collections found in configuration file and {} collections found in the mongodb", mongoPathFactory.size(), collections.size());
        }
        Tenant finalTenantTemp = tenantTemp;
        return mongoPathFactory.parallelStream().collect(Collectors.toMap(CollectionPath::getName, mongoCollection -> {
            Query query = mongoPathFactory.querryBuilder.build(mongoCollection, finalTenantTemp);
            long count = mongoTemplate.count(query, mongoCollection.getName());
            logger.info(String.format("%s documents present in the %s collection from the cache, in the %s environment", count, mongoCollection.getName(), env));
            return count;
        }));

    }

    public String getDocumentCountFromCacheDetailsAndBackup(String env) {
        try {
            Files.createDirectories(CLONING_PATH);
        } catch (IOException e) {
            logger.error(e.toString());
        }
        MongoTemplate mongoTemplate =mongoFactory.getTemplate(env);

        Tenant tenantTemp = null;
        try {
            tenantTemp = dataLoader.readDataFromCacheFile(env);
        } catch (Exception e) {
            logger.error("Cannot get the details");
        }
        if (mongoPathFactory.size() != getAllCollections(env).size()) {
            logger.error("The collection size mismatch found!, {} collections found in configuration file and {} collections found in the mongodb", mongoPathFactory.size(), getAllCollections(env).size());
        }
        Tenant finalTenantTemp = tenantTemp;
        Set<String> taskRemaining = mongoPathFactory.stream().map(CollectionPath::getName).collect(Collectors.toSet());
        mongoPathFactory.parallelStream().forEach(mongoCollection -> {
            Query query = mongoPathFactory.querryBuilder.build(mongoCollection, finalTenantTemp);
            Path outputPath = CLONING_PATH.resolve(mongoCollection.getName() + ".bson");

            try (MongoCursor<Document> cursor = mongoTemplate.getCollection(mongoCollection.getName()).find(query.getQueryObject()).cursor()) {
                List<Document> batch = new ArrayList<>();
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= BATCH_SIZE) {// Process in batches of 1000 documents
                        writeInBson(outputPath, batch);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) { // Write any remaining documents
                    writeInBson(outputPath, batch);
                }
            }
            taskRemaining.remove(mongoCollection.getName());
            logger.info("Remaining collections are {}", taskRemaining);

            logger.info(String.format(mongoCollection.getName() + " completed"));
        });
        return "Successfully backed up";

    }
    public Map<String, Long> checkNotwrittenInWareHouse(String env){
        MongoTemplate mongoTemplate =mongoFactory.getTemplate(env);
        Set<String> collections = getAllCollections(env);
        Tenant tenantTemp = null;
        try {
            tenantTemp = dataLoader.readDataFromCacheFile(env);
        } catch (Exception e) {
            logger.error("Cannot able to load Cache for {} environment", env);
        }
        if (mongoPathFactory.size() != collections.size()) {
            logger.warn("The collection size mismatch found!, {} collections found in configuration file and {} collections found in the mongodb", mongoPathFactory.size(), collections.size());
        }
        Tenant finalTenantTemp = tenantTemp;
        return mongoPathFactory.parallelStream().collect(Collectors.toMap(CollectionPath::getName, mongoCollection -> {
            Query query = mongoPathFactory.querryBuilder.build(mongoCollection, finalTenantTemp);
            query.addCriteria(Criteria.where("notWrittenInWarehouse").exists(true));
            long count = mongoTemplate.count(query, mongoCollection.getName());
            logger.info(String.format("%s documents is not synced in the %s collection from the cache, in the %s environment", count, mongoCollection.getName(), env));
            return count;
        }));

    }

    public String dropAllCollections(String env) {
        MongoTemplate mongoTemplate =mongoFactory.getTemplate(env);
        mongoTemplate.getCollectionNames().parallelStream().forEach(collection->{
            mongoTemplate.dropCollection(collection);
            logger.info(String.format("Dropped the %s collection", collection));
        });
        return "Successfully dropped";
    }

    public String getDataFromCache(String env) {
        try {
            Tenant temp = dataLoader.readDataFromCacheFile(env);
            logger.info(String.format("Data returned from the cache for the %s environment is %s ", env, temp.toString()));
            return temp.toString();
        } catch (IOException e) {
            return String.format("Error in Getting the cache file for %s environment ", env) + e;
        }

    }

    public Set<String> getAllCollections(String env) {
        MongoTemplate mongoTemplate =mongoFactory.getTemplate(env);
        logger.info("All collections information has been retrieved for the {} environment", env);
        return mongoTemplate.getCollectionNames();
    }

    public Map<String, Integer> getDocumentCount(String env, WebSocketSession session) {
        MongoTemplate mongoTemplate =mongoFactory.getTemplate(env);
        return mongoTemplate.getCollectionNames().parallelStream()
                .collect(Collectors.toMap(
                        collectionName -> collectionName,
                        collectionName -> {
                            long count = mongoTemplate.getCollection(collectionName).countDocuments();
                            logger.info("{} documents are in {} collection in {}", count, collectionName, env);
                            if (null != session) {
                                try {
                                    synchronized (session) {
                                        session.sendMessage(new TextMessage(collectionName + count));
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            return (int) count;
                        }
                ));
    }

    public synchronized void writeInBson(Path filePath, List<Document> documents) {
        File backupFile = filePath.toFile();
        if (!backupFile.exists()) {
            try {
                assert backupFile.createNewFile();
            } catch (IOException e) {
                logger.error(e.toString());
            }
        }
        try (FileOutputStream fos = new FileOutputStream(backupFile)) {
            for (Document document : documents) {
                BasicOutputBuffer buffer = new BasicOutputBuffer();
                BsonWriter writer = new BsonBinaryWriter(buffer);


                // Use DocumentCodec to encode Document to BsonWriter
                new DocumentCodec().encode(writer, document, org.bson.codecs.EncoderContext.builder().isEncodingCollectibleDocument(true).build());

                writer.flush();
                fos.write(buffer.toByteArray());
            }
        } catch (Exception e) {
            logger.error(e.toString());
        }

    }
}