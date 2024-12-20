package com.homenetics.eagleeye.models;

import java.util.List;

import lombok.Data;

@Data
public class DeviceModelWrapper {
    private List<DeviceModel> devices;
    private Integer totalRecords;
    private Integer totalPages;
    private Integer currentPage;
}
