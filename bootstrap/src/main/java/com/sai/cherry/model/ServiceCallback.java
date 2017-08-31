package com.sai.cherry.model;

/**
 * Created by saipkri on 31/08/17.
 */
public interface ServiceCallback<T extends FlowContext<Y>, Y, O> {

    O doInService(T flowcontext);

    default void callback(final Service service, final T flowcontext) {
        flowcontext.put(service.key(), doInService(flowcontext));
    }
}
