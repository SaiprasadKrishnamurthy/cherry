package com.sai.cherry.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.cherry.config.Bootstrap;
import com.sai.cherry.model.ContextHolder;
import com.sai.cherry.model.Flow;
import com.sai.cherry.model.FlowContext;
import com.sai.cherry.model.FlowNode;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;

/**
 * Created by saipkri on 18/08/17.
 */
@Api("Cherry base REST API")
@RestController
@RefreshScope
@RequestMapping("api")
public class CherryApi {

    @Autowired
    private Bootstrap bootstrap;

    @Autowired
    private ContextHolder contextHolder;

    @Autowired
    private ApplicationContext applicationContext;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PostMapping("/model/{flowName}")
    public DeferredResult<?> model(@PathVariable("flowName") final String flowName, final @RequestBody Map payload) throws Exception {
        DeferredResult<Object> response = new DeferredResult<>();
        FlowContext<Map> flowContext = FlowContext.newContext(payload);
        // Set up the context
        contextHolder.setup(flowContext, flowName.trim());


        // First node in the flow.
        FlowNode first = contextHolder.flow(flowName).getEdges().get(0).getFrom();

        bootstrap.getVertx().eventBus().send(first.type(), flowContext.transactionId() + "|" + first.key());

        // React to the response.
        bootstrap.getVertx().eventBus().consumer("DONE|" + flowContext.transactionId(), msg -> {
            response.setResult(contextHolder.responseFor(flowContext));
        });
        return response;
    }

    @GetMapping("/flows")
    public Map<String, Flow> rules() {
        return contextHolder.getFlows();
    }
}
