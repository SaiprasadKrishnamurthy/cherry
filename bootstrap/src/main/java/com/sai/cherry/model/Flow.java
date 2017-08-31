package com.sai.cherry.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by saipkri on 31/08/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Flow {
    private String name;
    private List<FlowEdge> edges;
}
