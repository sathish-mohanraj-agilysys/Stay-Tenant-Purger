package com.agilysys.StayTenantPurger.Service;

import com.agilysys.StayTenantPurger.modal.DAO.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CoreDeleteSerive {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void deleteTenant(Tenant tenants) {
//        for(String tenant:tenants.getTenant()){
//            TokenHandler.getInstance()
//                    .cachedLogin(Credentials.PLATFORM_USERNAME, Credentials.PLATFORM_PASSWORD, "0");
//
//            ClientHelper.runUntil(
//                    () -> ClientFactory.getUserService(false).deleteUserByUsername(tenant, Credentials.STAY_USERNAME),
//                    null, 500, 30000, SystemErrorCode.UNAUTHORIZED);
//            logger.info("user deleted" + Credentials.STAY_USERNAME);
//            ClientHelper
//                    .runUntil(() -> ClientFactory.getTenantService(false).deleteTenant(tenant), null, 500,
//                            30000, SystemErrorCode.UNAUTHORIZED);
//            logger.info("tenant deleted" + tenant);
//        }

    }
}
