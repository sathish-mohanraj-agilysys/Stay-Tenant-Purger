package com.agilysys.StayTenantPurger.Config;

import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {
    //    @Value("${spring.data.mongodb.uriqa}")
//    private String qaUri;
//    @Value("${spring.data.mongodb.uriqa01}")
//    private String qa01Uri;
//    @Value("${spring.data.mongodb.uriqa_int}")
//    private String qa_intUri;
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


    @Bean(name = "mongoTemplateqa03")
    public MongoTemplate getMongoTemplateQa03() {
        return new MongoTemplate(MongoClients.create(qa03Uri), qa03Database);
    }
    @Primary
    @Bean(name = "mongoTemplateLab005")
    public MongoTemplate getMongoTemplateLab005() {
        return new MongoTemplate(MongoClients.create(lab005Uri), lab005Database);
    }
    @Bean(name = "mongoTemplateLab000")
    public MongoTemplate getMongoTemplateLab000() {
        return new MongoTemplate(MongoClients.create(lab000Uri), lab000Database);
    }
}
