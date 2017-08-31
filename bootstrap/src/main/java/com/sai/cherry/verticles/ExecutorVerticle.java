package com.sai.cherry.verticles;

import com.sai.cherry.callbackdelegate.CallbackUtil;
import com.sai.cherry.model.ContextHolder;
import com.sai.cherry.model.FlowNode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;


/**
 * Created by saipkri on 31/08/17.
 */
@Component
@Scope(SCOPE_PROTOTYPE)
public class ExecutorVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorVerticle.class);

    private final ContextHolder contextHolder;

    @Autowired
    public ExecutorVerticle(final ContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    @Override
    public void start(final Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        getVertx().eventBus().consumer("rest", this::exec);
    }

    private void exec(final Message<String> msg) {
        String payload = msg.body();
        String transactionId = payload.split("\\|")[0];
        String key = payload.split("\\|")[1];

        LOG.info(" \t Key: {}", key);
        List<FlowNode> nextNodes = contextHolder.nextNodesFor(transactionId, contextHolder.getFlowNodes().get(key));

        FlowNode flowNode = contextHolder.getFlowNodes().get(key);
        boolean hasAllPredecessorsFinishedExecution = contextHolder.hasAllPredecessorsFinishedExecution(transactionId, key);
        CallbackUtil.callback(flowNode.type()).accept(flowNode, contextHolder.contextFor(transactionId));

        if (nextNodes.isEmpty()) {
            getVertx().eventBus().send(CompletionVerticle.class.getName(), transactionId + "|" + ""); // Don't need to pass anything around.
        } else if (hasAllPredecessorsFinishedExecution) {
            nextNodes
                    .forEach(next -> callNextRule(transactionId, next));
        } else {
            LOG.info("\t\t ---  {} I'm still waiting, have more job to do ---- ", key);
        }
    }

    private EventBus callNextRule(final String transactionId, final FlowNode next) {
        return vertx.eventBus().send(next.type(), transactionId + "|" + next.key());
    }
}
