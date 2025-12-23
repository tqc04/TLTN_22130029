package com.example.order.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "warehouse")
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String address;
    private String province; // Tên tỉnh/thành phố
    private String commune;  // Tên xã/phường
    private Integer districtId; // GHN district_id
    private String wardCode;    // GHN ward_code

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCommune() { return commune; }
    public void setCommune(String commune) { this.commune = commune; }

    public Integer getDistrictId() { return districtId; }
    public void setDistrictId(Integer districtId) { this.districtId = districtId; }
    public String getWardCode() { return wardCode; }
    public void setWardCode(String wardCode) { this.wardCode = wardCode; }
} 