package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.modal.DAO.Tenant;
import com.agilysys.platform.user.model.Property;
import com.agilysys.pms.common.exceptions.system.SystemErrorCode;
import com.agilysys.qa.clients.ClientFactory;
import com.agilysys.qa.helpers.ClientHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class CoreDeleteSerive {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void deleteTenant(Tenant tenants) {
        for(String tenantId:tenants.getTenant()){
            Collection<Property> propertiesForTenant =
                    ClientFactory.getPropertyService().getPropertiesForTenant(tenantId);

            for (Property property : propertiesForTenant) {
                ClientHelper.runUntil(() -> ClientFactory.getPropertyService()
                                .deleteProperty(tenantId,property.getPropertyId()), null, 500, 30000,
                        SystemErrorCode.UNAUTHORIZED);
            }
        }


    }
}
