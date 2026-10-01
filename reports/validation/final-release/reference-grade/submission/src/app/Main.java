package app;

import benchkit.Json;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Private reference transport. Not part of the candidate kit. */
public final class Main {
    private static final Pattern ITEM=Pattern.compile("/v1/items/([^/]+)");
    private static final Pattern RESERVATION=Pattern.compile("/v1/reservations/([^/]+)");
    private static final Pattern ACTION=Pattern.compile("/v1/reservations/([^/]+)/(confirm|cancel)");
    public static void main(String[] args)throws Exception{
        Map<String,String> opts=new HashMap<>();
        Set<String> accepted=Set.of("--port","--data-dir","--clock","--now");
        for(int i=0;i<args.length;i++){
            String k=args[i];if(!accepted.contains(k)||i+1>=args.length||opts.containsKey(k))throw new IllegalArgumentException("Invalid arguments");opts.put(k,args[++i]);
        }
        int port=Integer.parseInt(opts.getOrDefault("--port","8080"));if(port<1||port>65535)throw new IllegalArgumentException("Invalid port");
        String clock=opts.getOrDefault("--clock","system");if(!clock.equals("manual")&&!clock.equals("system"))throw new IllegalArgumentException("Invalid clock");
        boolean manual=clock.equals("manual");long now=Long.parseLong(opts.getOrDefault("--now","1700000000000"));
        if(now<0||now>8000000000000000L)throw new IllegalArgumentException("Invalid initial time");
        Service service=new Service(Paths.get(opts.getOrDefault("--data-dir","data")),manual,now);
        HttpServer server;
        try{server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),128);}catch(Exception e){service.close();throw e;}
        ExecutorService workers=Executors.newFixedThreadPool(16);
        server.setExecutor(workers);
        String token=System.getenv("BENCH_TEST_TOKEN");
        server.createContext("/",exchange->handle(exchange,service,manual,token));
        Runtime.getRuntime().addShutdownHook(new Thread(()->{server.stop(0);workers.shutdownNow();try{service.close();}catch(IOException ignored){}}));
        server.start();System.out.println("READY "+port);
    }
    private static void handle(HttpExchange e,Service service,boolean manual,String token)throws IOException{
        int status=200;Object result;
        try{
            String path=e.getRequestURI().getRawPath(),method=e.getRequestMethod();
            Matcher item=ITEM.matcher(path),reservation=RESERVATION.matcher(path),action=ACTION.matcher(path);
            String route,arg=null;String expected;
            if(path.equals("/health")){route="health";expected="GET";}
            else if(path.equals("/v1/stats")){route="stats";expected="GET";}
            else if(path.equals("/v1/items")){route="createItem";expected="POST";}
            else if(path.equals("/v1/reservations")){route=method.equals("GET")?"list":"reserve";expected=method.equals("GET")?"GET":"POST";}
            else if(path.equals("/__test__/advance")&&manual){route="advance";expected="POST";}
            else if(action.matches()){route=action.group(2);arg=action.group(1);expected="POST";}
            else if(item.matches()){route="item";arg=item.group(1);expected="GET";}
            else if(reservation.matches()){route="reservation";arg=reservation.group(1);expected="GET";}
            else throw new Service.Fault(404,"NOT_FOUND","Unknown route");
            if(!method.equals(expected))throw new Service.Fault(405,"METHOD_NOT_ALLOWED","Method not allowed");
            if(route.equals("advance")&&(token==null||token.isEmpty()||!token.equals(e.getRequestHeaders().getFirst("X-Test-Token"))))throw new Service.Fault(403,"FORBIDDEN","Test token required");
            Map<String,String> query=parseQuery(e.getRequestURI().getRawQuery());
            if(!route.equals("list")&&!query.isEmpty())throw new Service.Fault(400,"INVALID_REQUEST","Unexpected query parameter");
            Map<String,Object> body=expected.equals("POST")?readBody(e):Map.of();
            Service.Reply reply=route.equals("health")?new Service.Reply(200,Map.of("status","ok")):service.execute(route,arg,body,query);
            status=reply.status();result=reply.body();
        }catch(Service.Fault f){status=f.status;result=Map.of("error",Map.of("code",f.code,"message",f.getMessage()));}
        catch(IOException f){status=503;result=Map.of("error",Map.of("code","STORAGE_ERROR","message","Storage operation failed"));}
        catch(Exception f){status=500;result=Map.of("error",Map.of("code","INTERNAL_ERROR","message","Unexpected server error"));f.printStackTrace(System.err);}
        byte[] bytes=Json.stringify(result).getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
        e.getResponseHeaders().set("Connection","close");
        try{e.sendResponseHeaders(status,bytes.length);try(OutputStream out=e.getResponseBody()){out.write(bytes);}}finally{e.close();}
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> readBody(HttpExchange e)throws IOException{
        String ct=e.getRequestHeaders().getFirst("Content-Type");
        if(ct==null||!ct.split(";",2)[0].trim().equalsIgnoreCase("application/json"))throw new Service.Fault(415,"UNSUPPORTED_MEDIA_TYPE","application/json required");
        byte[] bytes=e.getRequestBody().readNBytes(65537);
        if(bytes.length>65536)throw new Service.Fault(413,"PAYLOAD_TOO_LARGE","Request body too large");
        try{
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            Object parsed=Json.parse(text);if(!(parsed instanceof Map))throw new IllegalArgumentException();return (Map<String,Object>)parsed;
        }catch(IllegalArgumentException|CharacterCodingException ex){throw new Service.Fault(400,"INVALID_REQUEST","Invalid JSON body");}
    }
    private static Map<String,String> parseQuery(String text){
        Map<String,String> result=new HashMap<>();if(text==null||text.isEmpty())return result;
        for(String part:text.split("&",-1)){
            String[] pair=part.split("=",2);if(pair.length!=2)throw new Service.Fault(400,"INVALID_REQUEST","Invalid query");
            try{
                String k=URLDecoder.decode(pair[0],StandardCharsets.UTF_8),v=URLDecoder.decode(pair[1],StandardCharsets.UTF_8);
                if(result.containsKey(k))throw new IllegalArgumentException();result.put(k,v);
            }catch(IllegalArgumentException ex){throw new Service.Fault(400,"INVALID_REQUEST","Invalid query");}
        }return result;
    }
}
