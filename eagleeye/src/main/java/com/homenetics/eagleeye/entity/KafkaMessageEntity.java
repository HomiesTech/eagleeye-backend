package com.homenetics.eagleeye.entity;
import lombok.Data;

@Data
public class KafkaMessageEntity<T> {
    private String action;
    private T data;
}
