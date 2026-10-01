package app;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** Starting point only: intentionally not a completed solution. */
public final class Main {
    public static void main(String[] args) throws Exception {
        int port=8080;
        for(int i=0;i<args.length;i++)if(args[i].equals("--port"))port=Integer.parseInt(args[++i]);
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),128);
        server.createContext("/",e->{
            boolean health=e.getRequestURI().getPath().equals("/health") && e.getRequestMethod().equals("GET");
            byte[] body=(health?"{\"status\":\"ok\"}":"{\"error\":{\"code\":\"NOT_IMPLEMENTED\",\"message\":\"Implement the task\"}}").getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
            e.sendResponseHeaders(health?200:501,body.length);
            try(var out=e.getResponseBody()){out.write(body);}
            e.close();
        });
        server.start();
    }
}
