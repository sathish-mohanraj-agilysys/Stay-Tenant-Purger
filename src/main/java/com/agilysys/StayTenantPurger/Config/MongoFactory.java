package com.agilysys.StayTenantPurger.Config;

import com.agilysys.StayTenantPurger.modal.DAO.MongoConnection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

@Component
public class MongoFactory {
    private ObjectMapper objectMapper = new ObjectMapper();

    private static ArrayList<MongoConnection> MONGO_CONNECTIONS;

    public MongoFactory() {
        try {
            MONGO_CONNECTIONS = objectMapper.readValue(Paths.get(System.getProperty("user.dir"), "mongoConnection.json").toFile(), new TypeReference<ArrayList<MongoConnection>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Cannot start the service ,mongoConnections Missing");
        }
    }

    public  synchronized MongoTemplate getTemplate(String env) {
        for (MongoConnection connection : MONGO_CONNECTIONS) {
            if (connection.getEnvName().contains(env)) {
                MongoClient mongoClient = MongoClients.create(connection.getUrl());
                return new MongoTemplate(mongoClient, connection.getDbName());
            }
        }
        throw new RuntimeException("Cannot proceed further because there is no connection string found for the MongoDB environment: " + env);
    }
}
