import ballerina.net.http;
import ballerina.tracing;

service<http> helloWorld {

    resource sayHello (http:Connection conn, http:InRequest req) {
    	tracing:TracingDepth depth = tracing:getTracingDepth();
    	string spanId = startServiceSpan(req, depth, "server-receive");

    	startWorker(req, depth);

        http:OutResponse res = {};
        res.setStringPayload("Hello, World! This is the second ballerina");
        _ = conn.respond(res);
        println("sending...");

        _ = finishSpan(spanId, depth);
    }
}

function startWorker(http:InRequest req, tracing:TracingDepth depth) {
	worker w1 {
		string spanId = startSpan(req, depth, "w1");
		sleep(10000);
        println("Hello, World! #1");
        _ = finishSpan(spanId, depth);
   		 }

    worker w2 {
		string spanId = startSpan(req, depth, "w2");
		sleep(3000);
		startAnotherWorker(req, depth);
        println("Hello, World! #2");
        _ = finishSpan(spanId, depth);
   	}
}

function startAnotherWorker(http:InRequest req, tracing:TracingDepth depth) {
	worker w3 {
		string spanId = startSpan(req, depth, "w3");
		sleep(10000);
        println("Hello, World! #1");
        _ = finishSpan(spanId, depth);
   		 }

    worker w4 {
		string spanId = startSpan(req, depth, "w4");
		sleep(3000);
        println("Hello, World! #2");
        _ = finishSpan(spanId, depth);
   	}
}


function startServiceSpan(http:InRequest req, tracing:TracingDepth depth, string spanKind) (string){
    	string spanId;
    	if(depth != tracing:TracingDepth.NONE){
    		map tags = {};
    		tags["span.kind"] = spanKind;
    		tags["http.url"] = req.rawPath;
    		tags["http.method"] = req.method;
    		spanId = tracing:buildSpan(req, tags, "Service: helloWorld", true);
    	}
    	return spanId;
	}

function startSpan(http:InRequest req, tracing:TracingDepth depth, string spanName) (string){
    	string spanId;
    	if(depth != tracing:TracingDepth.NONE){
    		map tags = {};
    		tags["span.kind"] = "worker";
    		spanId = tracing:buildSpan(req, tags, "Worker : "+ spanName, true);
    	}
    	return spanId;
	}

function finishSpan(string spanId, tracing:TracingDepth depth) (boolean){
	  if(depth != tracing:TracingDepth.NONE){
    		return tracing:finishSpan(spanId);
    	} else {
    	    return false;
    	}
}
