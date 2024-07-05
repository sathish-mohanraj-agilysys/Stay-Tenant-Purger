package com.agilysys.StayTenantPurger.Config;

import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {
    @Value("${spring.data.mongodb.uriqa}")
    private String qaUri;
    @Value("${spring.data.mongodb.databaseqa}")
    private String qaDatabase;
    @Value("${spring.data.mongodb.uriqaint}")
    private String qaintUri;
    @Value("${spring.data.mongodb.databaseqaint}")
    private String qaintDatabase;
    @Value("${spring.data.mongodb.urirelease}")
    private String releaseUri;
    @Value("${spring.data.mongodb.databaserelease}")
    private String releaseDatabase;
    @Value("${spring.data.mongodb.uriqa02}")
    private String qa02Uri;
    @Value("${spring.data.mongodb.databaseqa02}")
    private String qa02Database;
    @Value("${spring.data.mongodb.uriqa03}")
    private String qa03Uri;
    @Value("${spring.data.mongodb.databaseqa03}")
    private String qa03Database;
    @Value("${spring.data.mongodb.uri005}")
    private String lab005Uri;
    @Value("${spring.data.mongodb.database005}")
    private String lab005Database;
    @Value("${spring.data.mongodb.uri000}")
    private String lab000Uri;
    @Value("${spring.data.mongodb.database000}")
    private String lab000Database;
    @Value("${spring.data.mongodb.uri002}")
    private String lab002Uri;
    @Value("${spring.data.mongodb.database002}")
    private String lab002Database;
    @Value("${spring.data.mongodb.uriui}")
    private String labuiUri;
    @Value("${spring.data.mongodb.databaseui}")
    private String labuiDatabase;
    @Value("${spring.data.mongodb.uri001}")
    private String lab001Uri;
    @Value("${spring.data.mongodb.database001}")
    private String lab001Database;

    @Bean(name = "qa02")
    public MongoTemplate getMongoTemplateQa02() {
        return new MongoTemplate(MongoClients.create(qa02Uri), qa02Database);
    }
    @Bean(name = "qa03")
    public MongoTemplate getMongoTemplateQa03() {
        return new MongoTemplate(MongoClients.create(qa03Uri), qa03Database);
    }
    @Bean(name = "release")
    public MongoTemplate getMongoTemplateRelease() {
        return new MongoTemplate(MongoClients.create(releaseUri), releaseDatabase);
    }
    @Bean(name = "qa")
    public MongoTemplate getMongoTemplateQa() {
        return new MongoTemplate(MongoClients.create(qaUri), qaDatabase);
    }
    @Bean(name = "qaint")
    public MongoTemplate getMongoTemplateQaInt() {
        return new MongoTemplate(MongoClients.create(qaintUri), qaintDatabase);
    }
    @Primary
    @Bean(name = "005")
    public MongoTemplate getMongoTemplateLab005() {
        return new MongoTemplate(MongoClients.create(lab005Uri), lab005Database);
    }
    @Bean(name = "000")
    public MongoTemplate getMongoTemplateLab000() {
        return new MongoTemplate(MongoClients.create(lab000Uri), lab000Database);
    }
    @Bean(name = "002")
    public MongoTemplate getMongoTemplateLab002() {
        return new MongoTemplate(MongoClients.create(lab002Uri), lab002Database);
    }
    @Bean(name = "001")
    public MongoTemplate getMongoTemplateLab001() {
        return new MongoTemplate(MongoClients.create(lab001Uri), lab001Database);
    }
    @Bean(name = "ui")
    public MongoTemplate getMongoTemplateLabui() {
        return new MongoTemplate(MongoClients.create(labuiUri), labuiDatabase);
    }
}
