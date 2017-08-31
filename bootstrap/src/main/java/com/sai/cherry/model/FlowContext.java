package com.sai.cherry.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * Created by saipkri on 31/08/17.
 */
public class FlowContext<T> {
    private ApplicationContext applicationContext;
    private ConcurrentSkipListMap<String, Object> variables = new ConcurrentSkipListMap<>();
    private T model;
    private ContextHolder contextHolder;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Getter
    @Setter
    private Stack<String> errorTraces = new Stack<>();

    @Getter
    @Setter
    private Flow flow;

    private String transactionId = UUID.randomUUID().toString();

    public void put(final String key, final Object value) {
        this.variables.put(key, value);
    }

    public ApplicationContext applicationContext() {
        return applicationContext;
    }

    public T model() {
        return model;
    }

    public <O> O toTypedModel(final Class<O> type) {
        return objectMapper.convertValue(model, type);
    }

    public Object get(final String key) {
        return this.variables.get(key);
    }

    public String transactionId() {
        return transactionId;
    }

    public List<Object> predecessorValuesFor(final String key) {
        return contextHolder.prevNodesFor(transactionId, key)
                .stream()
                .map(flowNode -> get(flowNode.key()))
                .collect(Collectors.toList());
    }

    public Object recentValue() {
        return variables.lastEntry().getValue();
    }

    /**
     * Does this flow have mutiple converging points.
     *
     * @return
     */
    public boolean isForkJoinStyle() {
        return contextHolder.isForkJoinStyle(transactionId, flow);
    }

    public static <T> FlowContext<T> newContext(final T payload) {
        FlowContext<T> tRuleExecutionContext = new FlowContext<>();
        tRuleExecutionContext.model = payload;
        return tRuleExecutionContext;
    }
}
