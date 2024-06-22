package com.agilysys.StayTenantPurger.Config;

import com.agilysys.StayTenantPurger.DAO.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

@Component
public class DataLoader {
    @Autowired
    ObjectMapper  objectMapper;
    Yaml yaml = new Yaml();
    private Path RESOURCE_PATH = Paths.get(System.getProperty("user.dir"), "src", "main", "resources");
    private Path BACKUP_PATH = RESOURCE_PATH.resolve("Backup");
    public String YAML_NAME="rGuestStaymap.yml";

    public synchronized File loadYMlFile() {
        return RESOURCE_PATH.resolve(YAML_NAME).toFile();
    }

    public synchronized File loadCacheFile(String env) {
        return RESOURCE_PATH.resolve(env + ".json").toFile();
    }
    public synchronized Tenant readDataFromCacheFile(String env) throws IOException {
         return objectMapper.readValue(RESOURCE_PATH.resolve(env + ".json").toFile(), Tenant.class);
    }
    public synchronized Tenant readDataFromBackupFile(String env) throws IOException {
        return objectMapper.readValue(BACKUP_PATH.resolve(env + ".json").toFile(), Tenant.class);
    }
    public synchronized void writeDataIntoBackupFile(String env,Tenant tenant) throws IOException {
        objectMapper.writeValue(BACKUP_PATH.resolve(env + ".json").toFile(), tenant);
    }
    public synchronized void writeDataIntoCacheFile(String env,Tenant tenant) throws IOException {
         objectMapper.writeValue(RESOURCE_PATH.resolve(env + ".json").toFile(), tenant);
    }
    public synchronized  Map<String, Map<String, ArrayList<String>>> readYMlFile() throws FileNotFoundException {
       return yaml.load(new FileInputStream(this.loadYMlFile()));
    }

}
