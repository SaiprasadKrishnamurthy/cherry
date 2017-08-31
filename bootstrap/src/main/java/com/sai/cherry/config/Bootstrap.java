package com.sai.cherry.config;

import com.sai.cherry.verticles.CompletionVerticle;
import com.sai.cherry.verticles.ExecutorVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by saipkri on 14/04/17
 *
 */
@Component
@Configuration
@Data
public class Bootstrap {

    @Value("${executorInstances}")
    private int ruleExecutorInstances;

    @Autowired
    private final ApplicationContext applicationContext;

    private Vertx vertx;

    @PostConstruct
    public void onstartup() throws Exception {
        vertx = Vertx.vertx();
        VerticleFactory verticleFactory = applicationContext.getBean(SpringVerticleFactory.class);
        vertx.registerVerticleFactory(verticleFactory);
        vertx.deployVerticle(verticleFactory.prefix() + ":" + ExecutorVerticle.class.getName(), new DeploymentOptions().setInstances(ruleExecutorInstances).setWorker(true));
        vertx.deployVerticle(verticleFactory.prefix() + ":" + CompletionVerticle.class.getName(), new DeploymentOptions().setInstances(ruleExecutorInstances));
    }
}
