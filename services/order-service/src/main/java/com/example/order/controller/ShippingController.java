package com.example.order.controller;

import com.example.order.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    @Autowired
    private ShippingService shippingService;

    @Value("${GHN_API_TOKEN:}")
    private String ghnToken;
    @Value("${GHN_SHOP_ID:}")
    private String ghnShopId;

    private static final String GHN_MASTER_DATA_BASE_URL = "https://online-gateway.ghn.vn/shiip/public-api/master-data";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Helper method to create GHN headers
     */
    private HttpHeaders createGhnHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        
        if (ghnShopId != null && !ghnShopId.isBlank()) {
            headers.set("ShopId", ghnShopId);
        }
        
        return headers;
    }

    /**
     * Helper method to call GHN API and parse response
     */
    private ResponseEntity<?> callGhnApi(String endpoint, HttpMethod method, String body) {
        try {
            if (ghnToken == null || ghnToken.isBlank() || ghnToken.equals("YOUR_TOKEN_HERE")) {
                return ResponseEntity.status(500).body(Map.of("error", "GHN_API_TOKEN is missing. Set it in .env"));
            }
            
            HttpHeaders headers = createGhnHeaders();
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            String url = GHN_MASTER_DATA_BASE_URL + endpoint;
            
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> json = (Map<String, Object>) objectMapper.readValue(response.getBody(), Map.class);
            return ResponseEntity.ok(json);
            
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode().value()).body(Map.of(
                    "error", "GHN API error",
                    "status", e.getStatusCode().value(),
                    "response", e.getResponseBodyAsString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "service", "ShippingController",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/calculate-fee-v2")
    public ResponseEntity<?> calculateFeeV2(@RequestBody Map<String, Object> payload) {
        try {
            // FE truyền đúng DistrictID/WardCode GHN
            Integer fromDistrictId = null;
            Integer toDistrictId = null;
            String fromWardCode = null;
            String toWardCode = null;
            double weight = 1000;
            if (payload.get("fromDistrictId") != null) fromDistrictId = Integer.parseInt(payload.get("fromDistrictId").toString());
            if (payload.get("toDistrictId") != null) toDistrictId = Integer.parseInt(payload.get("toDistrictId").toString());
            if (payload.get("fromWardCode") != null) fromWardCode = payload.get("fromWardCode").toString();
            if (payload.get("toWardCode") != null) toWardCode = payload.get("toWardCode").toString();
            if (payload.get("weight") != null) weight = Double.parseDouble(payload.get("weight").toString());

            if (fromDistrictId == null || toDistrictId == null || fromWardCode == null || toWardCode == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "fromDistrictId, toDistrictId, fromWardCode, toWardCode là bắt buộc"));
            }

            ShippingService.ShippingResult result = shippingService.calculateGhnShippingFee(
                    fromDistrictId, fromWardCode, toDistrictId, toWardCode, weight
            );

            return ResponseEntity.ok(Map.of(
                    "fee", result.fee,
                    "leadtime", result.leadtime
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // GHN API endpoints (original implementation)
    @GetMapping("/ghn/provinces")
    public ResponseEntity<?> getGhnProvinces() {
        return callGhnApi("/province", HttpMethod.GET, null);
    }

    @GetMapping("/ghn/districts")
    public ResponseEntity<?> getGhnDistricts(@RequestParam("province_id") Integer provinceId) {
            String body = "{\"province_id\": " + provinceId + "}";
        return callGhnApi("/district", HttpMethod.POST, body);
    }

    @GetMapping("/ghn/wards")
    public ResponseEntity<?> getGhnWards(@RequestParam("district_id") Integer districtId) {
        String body = "{\"district_id\": " + districtId + "}";
        return callGhnApi("/ward", HttpMethod.POST, body);
    }

    // Frontend-compatible endpoints (without /ghn prefix)
    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        return callGhnApi("/province", HttpMethod.GET, null);
    }

    @GetMapping("/provinces/{provinceCode}/communes")
    public ResponseEntity<?> getCommunesByProvince(@org.springframework.web.bind.annotation.PathVariable String provinceCode) {
        try {
            // First convert province code to province ID if needed
            Integer provinceId = getProvinceIdFromCode(provinceCode);
            if (provinceId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid province code"));
            }

            String body = "{\"province_id\": " + provinceId + "}";
            return callGhnApi("/district", HttpMethod.POST, body);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/districts/getByProvince")
    public ResponseEntity<?> getDistrictsByProvince(@RequestParam("provinceCode") String provinceCode) {
        try {
            Integer provinceId = getProvinceIdFromCode(provinceCode);
            if (provinceId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid province code"));
            }

            String body = "{\"province_id\": " + provinceId + "}";
            return callGhnApi("/district", HttpMethod.POST, body);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/wards/getByDistrict")
    public ResponseEntity<?> getWardsByDistrict(@RequestParam("districtCode") String districtCode) {
        try {
            Integer districtId = getDistrictIdFromCode(districtCode);
            if (districtId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid district code"));
            }

            String body = "{\"district_id\": " + districtId + "}";
            return callGhnApi("/ward", HttpMethod.POST, body);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Helper methods to convert codes to IDs (simplified - in production you might want to cache these)
    private Integer getProvinceIdFromCode(String provinceCode) {
        // This is a simplified mapping - in production you'd want a proper lookup
        // For now, return null to indicate the code needs to be handled differently
        try {
            return Integer.parseInt(provinceCode);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getDistrictIdFromCode(String districtCode) {
        try {
            return Integer.parseInt(districtCode);
        } catch (NumberFormatException e) {
            return null;
        }
    }
} 