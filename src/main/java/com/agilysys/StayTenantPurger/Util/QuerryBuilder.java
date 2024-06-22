package com.agilysys.StayTenantPurger.Util;

import com.agilysys.StayTenantPurger.DAO.CollectionPath;
import com.agilysys.StayTenantPurger.DAO.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class QuerryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(QuerryBuilder.class);

    public synchronized Query build(CollectionPath mongoCollection, Tenant tenant) {

        Criteria criteria = null;
        if (mongoCollection.getName().equalsIgnoreCase("config") || mongoCollection.getName().equalsIgnoreCase("configEvents")) {
            assert tenant != null;
            Set<String> tenantAndProperty = tenant.getProperty();
            tenantAndProperty.addAll(tenant.getTenant());
            if (tenantAndProperty.isEmpty()) {
                logger.info("No doucuments found for the " + mongoCollection.getName());
               criteria= Criteria.where("path").is("");
            }else {
                String regex = tenantAndProperty.stream().collect(Collectors.joining("|"));
                criteria = Criteria.where("path").regex(regex);
            }

        } else if (!mongoCollection.getTenantPath().equalsIgnoreCase("") && !mongoCollection.getPropertyPath().equalsIgnoreCase("")) {
            criteria = new Criteria().orOperator(
                    Criteria.where(mongoCollection.getTenantPath()).in(tenant.getTenant()),
                    Criteria.where(mongoCollection.getPropertyPath()).in(tenant.getProperty())
            );
        } else if (mongoCollection.getTenantPath().equalsIgnoreCase("") && !mongoCollection.getPropertyPath().equalsIgnoreCase("")) {
            criteria = new Criteria().orOperator(

                    Criteria.where(mongoCollection.getPropertyPath()).in(tenant.getProperty())
            );
        } else {
            criteria = new Criteria().orOperator(
                    Criteria.where(mongoCollection.getTenantPath()).in(tenant.getTenant()));
        }
        return new Query(criteria);
    }

}
