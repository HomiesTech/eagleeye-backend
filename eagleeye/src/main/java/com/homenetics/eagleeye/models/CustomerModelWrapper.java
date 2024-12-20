package com.homenetics.eagleeye.models;

import lombok.Data;

@Data
public class CustomerModelWrapper {
    private CustomerModel[] data;
    private Integer totalRecords;
    private Integer totalPages;
    private Integer currentPage;
}
