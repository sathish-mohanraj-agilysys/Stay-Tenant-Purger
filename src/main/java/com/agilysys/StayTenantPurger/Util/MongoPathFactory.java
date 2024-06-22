package com.agilysys.StayTenantPurger.Util;

import com.agilysys.StayTenantPurger.Config.DataLoader;
import com.agilysys.StayTenantPurger.modal.DAO.CollectionPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;

@Component
public class MongoPathFactory extends ArrayList<CollectionPath> {
    @Autowired
    public QuerryBuilder querryBuilder;

    public MongoPathFactory(DataLoader dataLoader) throws FileNotFoundException {
        Map<String, Map<String, ArrayList<String>>> dbSchema = dataLoader.readYMlFile();
        dbSchema.entrySet().parallelStream().forEach(entry -> {
            String tenantPath = "";
            String propertyPath = "";
            if (entry.getValue().get("tenantId") != null) {
                tenantPath = entry.getValue().get("tenantId").stream().reduce((s1, s2) -> s1 + "." + s2).orElse("");
            }
            if (entry.getValue().get("propertyId") != null) {
                propertyPath = entry.getValue().get("propertyId").stream().reduce((s1, s2) -> s1 + "." + s2).orElse("");
            }
            CollectionPath collectionPath = new CollectionPath(entry.getKey(), tenantPath, propertyPath);
            this.add(collectionPath);
        });


    }
}
