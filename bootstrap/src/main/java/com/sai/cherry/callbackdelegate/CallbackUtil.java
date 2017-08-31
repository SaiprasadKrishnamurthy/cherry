package com.sai.cherry.callbackdelegate;

import com.sai.cherry.model.FlowContext;
import com.sai.cherry.model.FlowNode;
import com.sai.cherry.model.Service;
import com.sai.cherry.model.ServiceCallback;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by saipkri on 31/08/17.
 */
public class CallbackUtil {

    private static Map<String, BiConsumer<FlowNode, FlowContext<?>>> functions = new HashMap<>();

    private static BiConsumer<FlowNode, FlowContext<?>> serviceCallback = ((flowNode, flowContext) -> {
        try {
            Service service = (Service) flowNode;
            String clazz = service.getProperties().get("className").toString();
            ServiceCallback serviceCallbackInstance = (ServiceCallback) Class.forName(clazz).newInstance();
            serviceCallbackInstance.callback(service, flowContext);
        } catch (Exception ex) {
            flowContext.getErrorTraces().push(ExceptionUtils.getFullStackTrace(ex));
        }
    });

    static {
        functions.put("service", serviceCallback);
    }

    public static BiConsumer<FlowNode, FlowContext<?>> callback(final String type) {
        return functions.get(type);
    }

}
