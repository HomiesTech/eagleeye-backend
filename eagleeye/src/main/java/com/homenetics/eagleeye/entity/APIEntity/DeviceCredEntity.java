package com.homenetics.eagleeye.entity.APIEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DeviceCredEntity {
    @JsonProperty("def_dev_ssid")
    private String ssid;
    @JsonProperty("def_dev_password")
    private String password;
}
