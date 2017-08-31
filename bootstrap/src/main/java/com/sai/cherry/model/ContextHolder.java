package com.sai.cherry.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class ContextHolder {
    // Auto expire-able LRU cache
    private Cache<String, List<FlowEdge>> flowEdgesCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();
    private Cache<String, FlowContext> contextsCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();
    private Cache<String, AtomicInteger> predecessorsCountTrackCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    @Getter
    private Map<String, Flow> flows = new HashMap<>();

    @Getter
    private Map<String, FlowNode> flowNodes = new HashMap<>();

    private Cache<String, Object> responses = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    public FlowContext<?> contextFor(final String transactionId) {
        return contextsCache.getIfPresent(transactionId);
    }

    @PostConstruct
    public void loadFlowDefinitions() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        FlowDefinitions flowDefs = objectMapper.readValue(ContextHolder.class.getClassLoader().getResourceAsStream("flowdefs.yml"), FlowDefinitions.class);
        flowDefs.getFlows().stream()
                .forEach(fd -> {
                    String flowName = fd.getName().trim();
                    String flowDsl = Stream.of(fd.getDefs()).collect(Collectors.joining("\n"));
                    Flow flow = new Flow(flowName, edges(flowDsl.trim()));
                    flows.put(flowName, flow);
                    flow.getEdges().stream()
                            .flatMap(flowEdge -> Stream.of(flowEdge.getFrom(), flowEdge.getTo()))
                            .forEach(flowNode -> flowNodes.put(flowNode.key(), flowNode));
                });
    }

    public Flow flow(final String flowName) {
        return flows.get(flowName.trim());
    }

    public void setup(final FlowContext<?> flowContext, final String flowName) {
        Flow flow = flows.get(flowName.trim());
        flowEdgesCache.put(flowContext.transactionId(), flow.getEdges());
        _setup(flowContext, flow);

    }

    private void _setup(final FlowContext<?> flowContext, final Flow flow) {
        contextsCache.put(flowContext.transactionId(), flowContext);
        flowContext.setFlow(flow);
        flow.getEdges().stream()
                .flatMap(edge -> Stream.of(edge.getFrom(), edge.getTo()))
                .forEach(flowNode ->
                        predecessorsCountTrackCache.put(flowContext.transactionId() + "|" + flowNode.key(),
                                new AtomicInteger(prevNodesFor(flowContext.transactionId(), flowNode.key()).size())));
    }

    public List<FlowNode> nextNodesFor(final String transactionId, final FlowNode flowNode) {
        AtomicInteger counter = predecessorsCountTrackCache.getIfPresent(transactionId + "|" + flowNode.key());
        if (counter != null) {
            counter.decrementAndGet();
        }

        List<FlowEdge> edges = flowEdgesCache.getIfPresent(transactionId);
        if (edges == null) {
            return Collections.emptyList();
        }
        return edges.stream()
                .filter(edge -> edge.getFrom().key().equals(flowNode.key()))
                .filter(edge -> edge.getTo() != null)
                .map(FlowEdge::getTo)
                .collect(Collectors.toList());
    }

    public List<FlowNode> prevNodesFor(final String transactionId, final String nodeKey) {
        List<FlowEdge> edges = flowEdgesCache.getIfPresent(transactionId);
        if (edges == null) {
            return Collections.emptyList();
        }
        return edges.stream()
                .filter(edge -> edge.getFrom() != null)
                .filter(edge -> edge.getTo().key().equals(nodeKey))
                .map(FlowEdge::getFrom)
                .collect(Collectors.toList());
    }

    public void saveResponse(final FlowContext<?> flowContext, final Object response) {
        responses.put(flowContext.transactionId(), response);
    }

    public Object responseFor(final FlowContext<?> flowContext) {
        return responses.getIfPresent(flowContext.transactionId());
    }

    private List<FlowEdge> edges(final String dsl) {
        List<FlowEdge> edges = new ArrayList<>();
        Stream.of(dsl.split("\n"))
                .forEach(line -> Stream.of(line.split("->"))
                        .reduce((a, b) -> {
                            FlowEdge edge = new FlowEdge();
                            edge.setFrom(flowNode(a.trim()));
                            edge.setTo(flowNode(b.trim()));
                            edges.add(edge);
                            return b;
                        }));
        return edges;
    }

    private FlowNode flowNode(final String from) {
        String prefix = from.split(":")[0].trim();
        String remaining = from.split(":")[1].replace("(", "").replace(")", "").trim();

        switch (prefix) {
            case "rest": {
                return new Rest(new Properties(), remaining);
            }
            case "validator": {
                Properties properties = getProperties(remaining);
                return new Validator(properties, remaining);
            }
            case "service": {
                Properties properties = getProperties(remaining);
                return new Service(properties, remaining);
            }
            case "mongoRepository": {
                Properties properties = getProperties(remaining);
                return new MongoRepository(properties, remaining);
            }
            case "esRepository": {
                Properties properties = getProperties(remaining);
                return new EsRepository(properties, remaining);
            }
            case "jmsListener": {
                Properties properties = getProperties(remaining);
                return new JmsListener(properties, remaining);
            }
            default:
                throw new IllegalArgumentException("Unknown prefix: " + prefix);
        }
    }

    private Properties getProperties(final String remaining) {
        Properties properties = new Properties();
        Stream.of(remaining.split(",")).forEach(kv -> {
            properties.put(kv.split("=")[0].trim(), kv.split("=")[1].trim());
        });
        return properties;
    }

    public boolean isForkJoinStyle(final String transactionId, final Flow flow) {
        return flow.getEdges().stream()
                .flatMap(flowEdge -> Stream.of(flowEdge.getFrom(), flowEdge.getTo()))
                .filter(Objects::nonNull)
                .anyMatch(flowNode -> prevNodesFor(transactionId, flowNode.key()).size() > 1);
    }

    public boolean hasAllPredecessorsFinishedExecution(final String transactionId, final String key) {
        return predecessorsCountTrackCache.getIfPresent(transactionId + "|" + key).get() <= 0;
    }

    public static void main(String[] args) throws Exception {
        ContextHolder contextHolder = new ContextHolder();
        contextHolder.loadFlowDefinitions();
        FlowContext<?> fc = new FlowContext<>();
        contextHolder.setup(fc, "Flow Definition 1");
        System.out.println(contextHolder.predecessorsCountTrackCache.asMap());
        System.out.println(contextHolder.nextNodesFor(fc.transactionId(), new Service(null, "className = com.sai.foo.ABC")));
    }

}
