package com.homenetics.eagleeye.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.homenetics.eagleeye.util.HashUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerModel {
    private static final HashUtil hashUtil = new HashUtil();

    private int id;
    private String email;
    private String name;
    private String mobile;
    private String password;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String code;

    @JsonProperty("email_verified")
    private boolean emailVerified;

    public void setCode() {
        this.code = hashUtil.generateEmailHash(email);
    }
}
