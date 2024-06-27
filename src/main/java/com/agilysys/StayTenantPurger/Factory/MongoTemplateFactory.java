package com.agilysys.StayTenantPurger.Factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
@Configuration
public class MongoTemplateFactory {
    @Autowired
    public ApplicationContext context;

    public MongoTemplate getTemplate(String env) {
        return (MongoTemplate) context.getBean(env);

    }
}
