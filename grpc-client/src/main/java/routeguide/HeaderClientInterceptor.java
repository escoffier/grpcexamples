package routeguide;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderClientInterceptor implements ClientInterceptor {
    private static final Logger logger  = LoggerFactory.getLogger(HeaderClientInterceptor.class);
    static final Metadata.Key<String> CUSTOM_HEADER_KEY =
            Metadata.Key.of("custom_client_header_key", Metadata.ASCII_STRING_MARSHALLER);


    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(CUSTOM_HEADER_KEY, "customRequestValue");
                super.start(new CustomForwardingClientCallListener(responseListener), headers);
            }
        };
    }

    private class CustomForwardingClientCallListener extends ForwardingClientCallListener.SimpleForwardingClientCallListener {
        protected CustomForwardingClientCallListener(ClientCall.Listener delegate) {
            super(delegate);
        }

        @Override
        public void onHeaders(Metadata headers) {
            logger.debug("#################### header received from server: " + headers.toString());
            super.onHeaders(headers);
        }

        @Override
        public void onMessage(Object message) {
            logger.debug("&&&&&####################&&&&&& message received from server: " + message.toString());
            super.onMessage(message);
        }
    }
}
