package com.sai.cherry.config;

import com.google.common.base.Predicates;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.sai.cherry.jms.JmsEventListener;
import com.sai.cherry.model.ContextHolder;
import com.sai.cherry.model.Flow;
import com.sai.cherry.model.JmsListener;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.inject.Inject;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Created by saipkri on 18/08/17.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.sai.cherry", "${springBeansPkgs:''}"})
@EnableMongoRepositories
@EnableSwagger2
@EnableAsync
@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients
public class CherryApp implements JmsListenerConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CherryApp.class);

    @Value("${mongoHost ?: localhost}")
    private String mongoHost;

    @Value("${mongoPort ?: 27017}")
    private int mongoPort;

    @Value("${esUrl ?: localhost:9200}")
    private String esUrl;

    @Value("${esIndexBatchSize ?: 1}")
    private int esIndexBatchSize;

    @Inject
    private ApplicationContext applicationContext;

    @Value("${amqBrokerUrl ?:tcp://localhost:61616}")
    private String amqBrokerUrl;

    @Value("${sqlDbConnectionPoolSize ?: 30}")
    private int sqlDbConnectionPoolSize;

    @Value("${jdbcUrl ?: jdbc:h2:mem:strawberry-memdb;DB_CLOSE_DELAY=-1}")
    private String jdbcUrl;

    @Value("${jdbcDriver ?: org.h2.Driver}")
    private String jdbcDriver;

    @Value("${jdbcUser ?: #{null}}")
    private String jdbcUser;

    @Value("${jdbcPassword ?: #{null}}")
    private String jdbcPassword;

    @Autowired
    private ContextHolder contextHolder;

    @Autowired
    private Bootstrap bootstrap;

    @Value("${mongoDb ?: cherry}")
    private String mongoDb;

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public MongoTemplate mongoTemplate(final MongoDbFactory mongoDbFactory, final MappingMongoConverter mappingMongoConverter) {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory, mappingMongoConverter);
        return mongoTemplate;
    }

    @Bean
    public MongoDbFactory getMongoDbFactory() throws Exception {
        MongoClient mongoClient = new MongoClient(mongoHost, MongoClientOptions.builder().connectTimeout(100000).socketTimeout(1000000).heartbeatConnectTimeout(1000000).heartbeatSocketTimeout(1000000).maxWaitTime(1000000).build());
        return new SimpleMongoDbFactory(mongoClient, mongoDb);
    }

    @Bean
    public MappingMongoConverter mappingMongoConvertor(MongoDbFactory mongoDbFactory) {
        MappingMongoConverter mappingMongoConverter = new MappingMongoConverter(mongoDbFactory, new MongoMappingContext());
        mappingMongoConverter.setMapKeyDotReplacement("##");
        return mappingMongoConverter;
    }


    @Bean
    public JdbcTemplate jdbcTemplate() {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(jdbcUrl);
        ds.setDriverClassName(jdbcDriver);
        ds.setInitialSize(sqlDbConnectionPoolSize);
        ds.setUsername(jdbcUser);
        ds.setPassword(jdbcPassword);
        ds.setPoolPreparedStatements(true);
        ds.setMaxOpenPreparedStatements(sqlDbConnectionPoolSize);
        return new JdbcTemplate(ds);
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        return new JmsTemplate(connectionFactory());
    }

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        System.out.println(" ------- ActiveMQConnectionFactory Factory ----------");
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(amqBrokerUrl);
        return connectionFactory;
    }


    /**
     * Swagger 2 docket bean configuration.
     *
     * @return swagger 2 Docket.
     */
    @Bean
    public Docket configApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("config")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(Predicates.not(PathSelectors.regex("/error"))) // Exclude Spring error controllers
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Cherry REST API")
                .contact(new Contact("Sai", "http://studentpodium.com", "saiprasad.k@studentpodium.com"))
                .version("1.0")
                .build();
    }

    @Bean
    public DefaultJmsListenerContainerFactory defaultContainerFactory(ActiveMQConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrency("1-" + Runtime.getRuntime().availableProcessors());
        return factory;
    }

    @Override
    public void configureJmsListeners(final JmsListenerEndpointRegistrar jmsListenerEndpointRegistrar) {
        jmsListenerEndpointRegistrar.setContainerFactory(defaultContainerFactory(connectionFactory()));
        Collection<Flow> flows = contextHolder.getFlows().values();
        final JmsEventListener jmsEventListener = new JmsEventListener(bootstrap);

        flows.stream()
                .flatMap(flow -> flow.getEdges().stream())
                .flatMap(edge -> Stream.of(edge.getFrom(), edge.getTo())
                        .filter(flowNode -> (flowNode instanceof JmsListener))
                        .map(flowNode -> (JmsListener) flowNode))
                .forEach(flowNode -> {
                    SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
                    String destination = flowNode.properties().get("destination").toString();
                    endpoint.setDestination(destination);
                    endpoint.setMessageListener(jmsEventListener);
                    endpoint.setId(flowNode.key());
                    jmsListenerEndpointRegistrar.registerEndpoint(endpoint);
                    LOGGER.info("Spinning a JMS Listener dynamically for destination {}", destination);
                });
    }

    public static void main(String[] args) {
        SpringApplication.run(CherryApp.class);
    }
}
