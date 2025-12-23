package com.shoppro.warranty.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "order-service", fallback = OrderServiceClientFallback.class)
public interface OrderServiceClient {

    @GetMapping("/api/orders/number/{orderNumber}")
    Map<String, Object> getOrderByNumber(@PathVariable String orderNumber);
}
