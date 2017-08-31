package com.sai.cherry.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.ToString;

import java.util.Properties;

/**
 * Created by saipkri on 31/08/17.
 */
@ToString
@Data
public class Rest implements FlowNode {
    private final Properties properties;
    private final String key;

    @JsonCreator
    public Rest(final Properties properties, final String key) {
        this.properties = properties;
        this.key = key;
    }

    @Override
    public Properties properties() {
        return properties;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String type() {
        return "rest";
    }
}
