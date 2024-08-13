package com.agilysys.StayTenantPurger.modal.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ElasticsearchResponse {
    private int took;
    private boolean timed_out;
    private int total;
    private int deleted;
    private int batches;
    private int version_conflicts;
    private int noops;
    private long throttled_millis;
    private double requests_per_second;
    private long throttled_until_millis;
    private List<Object> failures;
}
