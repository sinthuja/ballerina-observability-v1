import ballerina.net.http;
import ballerina.tracing;
import ballerina.runtime;

service<http> helloWorld {
    resource sayHello (http:Connection conn, http:InRequest req) {
    	tracing:TracingDepth depth = tracing:getTracingDepth();
    	string spanId;
    	if(depth != tracing:TracingDepth.NONE){
    		map tags = {};
    		tags["span.kind"] = "server-receive";
    		tags["http.url"] = req.rawPath;
    		tags["http.method"] = req.method;
    		spanId = tracing:buildSpan(req, tags, "Service: helloWorld", true);
    	}

        http:OutResponse res = {};
        res.setStringPayload("Hello, World! This is the first ballerina");
        _ = conn.respond(res);

        if(depth != tracing:TracingDepth.NONE){
    	_ = tracing:finishSpan(spanId);
    	}
    }
}
