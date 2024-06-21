package com.agilysys.StayTenantPurger.Config;

import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DataLoader {
    public Path RESOURCE_PATH = Paths.get(System.getProperty("user.dir"), "src", "main", "resources");
    public Path BACKUP_PATH = RESOURCE_PATH.resolve("Backup");

    public File loadYMlFile() {
        return RESOURCE_PATH.resolve("rGuestStaymap.yml").toFile();

    }

    public synchronized File loadCacheFile(String env) {

        return RESOURCE_PATH.resolve(env + ".json").toFile();

    }
}
