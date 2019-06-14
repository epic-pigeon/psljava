import ParserPackage.Collection;
import ParserPackage.Environment;
import ParserPackage.PSLFunction;
import ParserPackage.Value;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class HTTPLibBuilder extends LibBuilder {
    public static void main(String[] args) throws Exception {
        HashMap<String, Value> exports = new HashMap<>();
        Value http = new Value();
        Value server = new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t, Environment environment) throws Exception {
                        PSLFunction handler = (PSLFunction) t.get(0).getValue();
                        String path = (String) t.get(1).getValue();
                        int port = ((Number) t.get(2).getValue()).intValue();
                        HttpServer httpServer = HttpServer.create();
                        httpServer.bind(new InetSocketAddress(port), 0);
                        HttpContext httpContext = httpServer.createContext(path, httpExchange -> {
                            httpExchange.sendResponseHeaders(200, 0);
                            Value request = new Value();
                            Value response = new Value();
                            String query = httpExchange.getRequestURI().getQuery();
                            Value queryValue = new Value();
                            for (String param : query.split("&")) {
                                String[] entry = param.split("=");
                                try {
                                    if (entry.length > 1) {
                                        queryValue.put(entry[0], new Value(entry[1]));
                                    } else {
                                        queryValue.put(entry[0], new Value(""));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                request.put("query", queryValue);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            CountDownLatch latch = new CountDownLatch(1);
                            OutputStream outputStream = httpExchange.getResponseBody();
                            try {
                                response.put("send", new Value(
                                        new PSLFunction() {
                                            @Override
                                            public Value apply(Collection<Value> t) throws Exception {
                                                for (Value value: t) {
                                                    outputStream.write(((String) value.getValue()).getBytes());
                                                }
                                                return Value.NULL;
                                            }
                                        }
                                ));
                                response.put("close", new Value(
                                        new PSLFunction() {
                                            @Override
                                            public Value apply(Collection<Value> t, Environment environment) throws Exception {
                                                outputStream.close();
                                                latch.countDown();
                                                return Value.NULL;
                                            }
                                        }
                                ));
                                handler.apply(new Collection<>(request, response), environment);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                        httpServer.setExecutor(null);
                        Value server = new Value();
                        server.put("listen", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        httpServer.start();
                                        return Value.NULL;
                                    }
                                }
                        ));
                        server.put("stop", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        httpServer.stop(0);
                                        return Value.NULL;
                                    }
                                }
                        ));
                        return server;
                    }
                }
        );
        http.put("server", server);
        exports.put("http", http);
        build("lib/http", exports);
    }
}
