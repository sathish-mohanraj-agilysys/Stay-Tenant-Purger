package com.agilysys.StayTenantPurger.modal.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaleCron {
    ArrayList<String> environments=new ArrayList<>();
    boolean isAutomationTenantsIncluded;
}
