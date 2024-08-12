package com.agilysys.StayTenantPurger.Util;

import java.util.Base64;

public class Base64Generator {
    public synchronized static String generateBasicAuth(String username, String password) {
        String authString = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes());
        return "Basic " + encodedAuth;
    }
}
