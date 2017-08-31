package com.sai.cherry.jms;

import com.sai.cherry.config.Bootstrap;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

/**
 * Created by saipkri on 28/11/16.
 */
public class JmsEventListener implements javax.jms.MessageListener {

    private final Bootstrap bootstrap;

    public JmsEventListener(final Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void onMessage(final Message message) {
        try {
            // TODO
            bootstrap.getVertx().eventBus().send("", ((TextMessage) message).getText());
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}