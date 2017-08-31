package com.sai.cherry.verticles;

import com.sai.cherry.model.ContextHolder;
import com.sai.cherry.model.FlowContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Created by saipkri on 18/08/17.
 */
@Component
@Scope(SCOPE_PROTOTYPE)
public class CompletionVerticle extends AbstractVerticle {

    private final ContextHolder contextHolder;

    @Autowired
    public CompletionVerticle(ContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    @Override
    public void start(final Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        getVertx().eventBus().consumer(CompletionVerticle.class.getName(), this::exec);
    }

    private void exec(final Message<String> msg) {
        String payload = msg.body();
        String transactionId = payload.split("\\|")[0];
        FlowContext<?> ruleExecutionContext = contextHolder.contextFor(transactionId);
        getVertx().eventBus().send("DONE|" + ruleExecutionContext.transactionId(), ""); // Don't need to pass anything around.
    }
}
