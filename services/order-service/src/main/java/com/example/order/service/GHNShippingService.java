package com.example.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GHNShippingService {
    
    private static final Logger logger = LoggerFactory.getLogger(GHNShippingService.class);
    
    @Value("${ghn.api.token:}")
    private String ghnToken;
    
    @Value("${ghn.api.shop-id:}")
    private String ghnShopId;
    
    @Value("${ghn.api.base-url:https://dev-online-gateway.ghn.vn/shiip/public-api}")
    private String ghnBaseUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Calculate shipping fee using GHN API
     */
    public Map<String, Object> calculateShippingFee(
            Integer fromDistrictId,
            String fromWardCode,
            Integer toDistrictId,
            String toWardCode,
            Integer weight,
            Integer insuranceValue
    ) {
        // Validate configuration
        if (isBlank(ghnToken) || isBlank(ghnShopId) || ghnToken.equals("YOUR_TOKEN_HERE")) {
            logger.warn("GHN API not configured, using default fee");
            return Map.of(
                "success", true,
                "fee", 30000,
                "leadtime", "2-4 days",
                "usingDefault", true
            );
        }
        
        try {
            String url = ghnBaseUrl + "/v2/shipping-order/fee";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnToken);
            headers.set("ShopId", ghnShopId);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from_district_id", fromDistrictId);
            requestBody.put("from_ward_code", fromWardCode);
            requestBody.put("to_district_id", toDistrictId);
            requestBody.put("to_ward_code", toWardCode);
            requestBody.put("weight", weight != null ? weight : 1000); // Default 1kg
            requestBody.put("insurance_value", insuranceValue != null ? insuranceValue : 0);
            requestBody.put("service_type_id", 2); // Standard service
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) response.getBody();
                
                if (body != null && body.get("code") != null && (Integer) body.get("code") == 200) {
                    Object dataObj = body.get("data");
                    if (dataObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        
                        Integer totalFee = (Integer) data.get("total");
                        String expectedDeliveryTime = (String) data.getOrDefault("expected_delivery_time", "2-4 days");
                        
                        return Map.of(
                            "success", true,
                            "fee", totalFee,
                            "leadtime", expectedDeliveryTime,
                            "usingDefault", false
                        );
                    }
                }
            }
            
            logger.error("GHN API returned error, using default fee");
            return getDefaultShippingFee();
            
        } catch (Exception e) {
            logger.error("Error calling GHN API: " + e.getMessage(), e);
            return getDefaultShippingFee();
        }
    }
    
    /**
     * Get list of provinces from GHN
     */
    public Map<String, Object> getProvinces() {
        if (isBlank(ghnToken) || ghnToken.equals("YOUR_TOKEN_HERE")) {
            return Map.of("success", false, "error", "GHN API not configured - please set GHN_API_TOKEN in environment variables");
        }
        
        try {
            String url = ghnBaseUrl + "/master-data/province";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", ghnToken);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody();
                return result;
            }
            
            return Map.of("success", false, "error", "Failed to fetch provinces");
        } catch (Exception e) {
            logger.error("Error fetching GHN provinces: " + e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Get list of districts by province ID from GHN
     */
    public Map<String, Object> getDistricts(Integer provinceId) {
        if (isBlank(ghnToken) || ghnToken.equals("YOUR_TOKEN_HERE")) {
            return Map.of("success", false, "error", "GHN API not configured - please set GHN_API_TOKEN in environment variables");
        }
        
        try {
            String url = ghnBaseUrl + "/master-data/district?province_id=" + provinceId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", ghnToken);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody();
                return result;
            }
            
            return Map.of("success", false, "error", "Failed to fetch districts");
        } catch (Exception e) {
            logger.error("Error fetching GHN districts: " + e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Get list of wards by district ID from GHN
     */
    public Map<String, Object> getWards(Integer districtId) {
        if (isBlank(ghnToken) || ghnToken.equals("YOUR_TOKEN_HERE")) {
            return Map.of("success", false, "error", "GHN API not configured - please set GHN_API_TOKEN in environment variables");
        }
        
        try {
            String url = ghnBaseUrl + "/master-data/ward?district_id=" + districtId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", ghnToken);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody();
                return result;
            }
            
            return Map.of("success", false, "error", "Failed to fetch wards");
        } catch (Exception e) {
            logger.error("Error fetching GHN wards: " + e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Get available shipping services
     */
    public Map<String, Object> getAvailableServices(Integer fromDistrict, Integer toDistrict) {
        if (isBlank(ghnToken) || isBlank(ghnShopId) || ghnToken.equals("YOUR_TOKEN_HERE")) {
            return Map.of("success", false, "error", "GHN API not configured - please set GHN_API_TOKEN in environment variables");
        }
        
        try {
            String url = ghnBaseUrl + "/v2/shipping-order/available-services";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnToken);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("shop_id", Integer.parseInt(ghnShopId));
            requestBody.put("from_district", fromDistrict);
            requestBody.put("to_district", toDistrict);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody();
                return result;
            }
            
            return Map.of("success", false, "error", "Failed to fetch services");
        } catch (Exception e) {
            logger.error("Error fetching GHN services: " + e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    private Map<String, Object> getDefaultShippingFee() {
        return Map.of(
            "success", true,
            "fee", 30000,
            "leadtime", "2-4 days",
            "usingDefault", true
        );
    }
    
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

