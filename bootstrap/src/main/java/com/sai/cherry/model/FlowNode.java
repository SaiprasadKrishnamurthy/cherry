package com.sai.cherry.model;

import java.util.Properties;

/**
 * Created by saipkri on 31/08/17.
 */
public interface FlowNode {
    Properties properties();
    String key();
    String type();
}
