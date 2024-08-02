package com.agilysys.StayTenantPurger.modal.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteQuery {
    private Query query;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Query {
        private Match match;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Match {
        private String tenantId;
    }
}
