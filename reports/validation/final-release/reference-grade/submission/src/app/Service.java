package app;

import benchkit.Journal;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Private reference: a single transaction lock, event journal, and deadline heap. */
public final class Service implements AutoCloseable {
    public static final class Fault extends RuntimeException{
        public final int status;public final String code;
        public Fault(int status,String code,String message){super(message);this.status=status;this.code=code;}
    }
    public record Reply(int status,Object body){}
    private static final Set<String> STATES=Set.of("ACTIVE","CONFIRMED","CANCELLED","EXPIRED");
    private static final String MUTANT=System.getenv("BENCH_MUTANT");
    private static boolean mutant(String id){return id.equals(MUTANT);}
    private static final class Item{
        final String sku;final long total;long available,held,sold;
        Item(String sku,long total){this.sku=sku;this.total=total;available=total;}
        Map<String,Object> view(){return obj("sku",sku,"total",total,"available",available,"held",held,"sold",sold);}
    }
    private record Line(String sku,long quantity){Map<String,Object> view(){return obj("sku",sku,"quantity",quantity);}}
    private static final class Reservation{
        final long id,createdAt,expiresAt,ttl;final String key;final List<Line> lines;String state="ACTIVE";
        Reservation(long id,String key,List<Line> lines,long ttl,long now){this.id=id;this.key=key;this.lines=List.copyOf(lines);this.ttl=ttl;createdAt=now;expiresAt=now+ttl;}
        Map<String,Object> view(){return obj("id",Long.toString(id),"requestId",key,"state",state,"createdAt",createdAt,"expiresAt",expiresAt,"lines",lines.stream().map(Line::view).toList());}
    }
    private final Journal journal;private final boolean manual;
    private final Map<String,Item> items=new HashMap<>();
    private final NavigableMap<Long,Reservation> reservations=new TreeMap<>();
    private final Map<String,Reservation> keys=new HashMap<>();
    private final PriorityQueue<Reservation> deadlines=new PriorityQueue<>(Comparator.comparingLong((Reservation r)->r.expiresAt).thenComparingLong(r->r.id));
    private long now,nextId;
    public Service(Path directory,boolean manual,long initialTime)throws IOException{
        this.manual=manual;this.now=0; journal=new Journal(directory);
        try{for(Object record:journal.records())apply(asMap(record)); now=Math.max(now,manual?initialTime:System.currentTimeMillis()); refreshTime();expire();}
        catch(RuntimeException e){journal.close();throw new IOException("Invalid journal event",e);}
    }
    @SuppressWarnings("unchecked")private static Map<String,Object> asMap(Object v){if(!(v instanceof Map))throw invalid();return (Map<String,Object>)v;}
    private static Fault invalid(){return new Fault(400,"INVALID_REQUEST","Invalid request field");}
    private static Map<String,Object> obj(Object...pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
    private static void fields(Map<String,Object> body,String...names){if(!body.keySet().equals(Set.of(names)))throw invalid();}
    private static String string(Object v,int max){if(!(v instanceof String s)||!s.matches("[A-Za-z0-9_-]{1,"+max+"}"))throw invalid();return s;}
    private static long number(Object v,long min,long max){if(!(v instanceof Long n)||n<min||n>max)throw invalid();return n;}
    private static long id(String v,boolean zero){if(v==null||!v.matches("[0-9]+"))throw invalid();try{long n=Long.parseLong(v);if(n<(zero?0:1))throw invalid();return n;}catch(NumberFormatException e){throw invalid();}}
    private static List<Line> parseLines(Object value){
        if(!(value instanceof List<?> list)||list.isEmpty()||list.size()>20)throw invalid();
        Set<String> seen=new HashSet<>();List<Line> result=new ArrayList<>();
        for(Object v:list){Map<String,Object> d=asMap(v);fields(d,"sku","quantity");String sku=string(d.get("sku"),32);long q=number(d.get("quantity"),mutant("M09")?0:1,1000000000);if(!seen.add(sku))throw invalid();result.add(new Line(sku,q));}
        result.sort(Comparator.comparing(Line::sku));return result;
    }
    private void refreshTime(){if(!manual)now=Math.max(now,System.currentTimeMillis());}
    private void expire(){
        while(!deadlines.isEmpty()&&(mutant("M04")?deadlines.peek().expiresAt<now:deadlines.peek().expiresAt<=now)){
            Reservation r=deadlines.poll();if(!r.state.equals("ACTIVE"))continue;
            release(r);r.state="EXPIRED";
        }
    }
    private void release(Reservation r){for(Line line:r.lines){Item item=items.get(line.sku);item.held-=line.quantity;item.available+=line.quantity;}}
    private Item item(String sku){Item i=items.get(sku);if(i==null)throw new Fault(404,"NOT_FOUND","Item not found");return i;}
    private Reservation reservation(String rid){Reservation r=reservations.get(id(rid,false));if(r==null)throw new Fault(404,"NOT_FOUND","Reservation not found");return r;}
    private void persist(Map<String,Object> event)throws IOException{event.put("at",now);journal.append(event);apply(event);}
    private void apply(Map<String,Object> e){
        now=Math.max(now,((Number)e.get("at")).longValue());expire();String type=(String)e.get("type");
        switch(type){
            case "item":{String sku=(String)e.get("sku");items.put(sku,new Item(sku,((Number)e.get("quantity")).longValue()));break;}
            case "reserve":{
                long rid=((Number)e.get("id")).longValue();List<Line> ls=parseLines(e.get("lines"));
                Reservation r=new Reservation(rid,(String)e.get("requestId"),ls,((Number)e.get("ttlMillis")).longValue(),((Number)e.get("at")).longValue());
                for(Line line:ls){Item item=items.get(line.sku);item.available-=line.quantity;item.held+=line.quantity;}
                reservations.put(rid,r);keys.put(r.key,r);deadlines.add(r);nextId=Math.max(nextId,rid);break;
            }
            case "transition":{
                Reservation r=reservations.get(((Number)e.get("id")).longValue());String target=(String)e.get("state");
                if(target.equals("CONFIRMED"))for(Line line:r.lines){Item i=items.get(line.sku);i.held-=line.quantity;i.sold+=line.quantity;if(mutant("M06"))i.available+=line.quantity;}
                else release(r);
                r.state=target;break;
            }
            case "clock":now=Math.max(now,((Number)e.get("now")).longValue());break;
            default:throw new IllegalArgumentException("Unknown journal event");
        }
        expire();
    }
    public synchronized Reply execute(String route,String arg,Map<String,Object> body,Map<String,String> query)throws IOException{
        refreshTime();expire();
        switch(route){
            case "createItem":{
                fields(body,"sku","quantity");String sku=string(body.get("sku"),32);long quantity=number(body.get("quantity"),0,1000000000);
                if(items.containsKey(sku))throw new Fault(409,"ITEM_EXISTS","Item already exists");
                persist(obj("type","item","sku",sku,"quantity",quantity));return new Reply(201,item(sku).view());
            }
            case "item":return new Reply(200,item(string(arg,32)).view());
            case "reservation":return new Reply(200,reservation(arg).view());
            case "reserve":{
                fields(body,"requestId","lines","ttlMillis");String key=string(body.get("requestId"),64);long ttl=number(body.get("ttlMillis"),1,86400000);List<Line> ls=parseLines(body.get("lines"));
                Reservation previous=keys.get(key);
                if(previous!=null&&!mutant("M02")){
                    if((!previous.lines.equals(ls)||previous.ttl!=ttl)&&!mutant("M03"))throw new Fault(409,"IDEMPOTENCY_CONFLICT","Request key has a different payload");
                    return new Reply(200,previous.view());
                }
                for(Line line:ls)item(line.sku);
                List<Line> prior=new ArrayList<>();
                for(Line line:ls){
                    if(item(line.sku).available<line.quantity&&!mutant("M01")){
                        if(mutant("M08"))for(Line done:prior){Item i=item(done.sku);i.available-=done.quantity;i.held+=done.quantity;}
                        throw new Fault(409,"INSUFFICIENT_STOCK","Not enough stock");
                    }prior.add(line);
                }
                long rid=nextId+1;
                persist(obj("type","reserve","id",rid,"requestId",key,"lines",ls.stream().map(Line::view).toList(),"ttlMillis",ttl));
                return new Reply(201,reservations.get(rid).view());
            }
            case "confirm":case "cancel":{
                fields(body);Reservation r=reservation(arg);String target=route.equals("confirm")?"CONFIRMED":"CANCELLED";
                if(r.state.equals(target)&&!(mutant("M05")&&route.equals("cancel")))return new Reply(200,r.view());
                boolean bad=r.state.equals("ACTIVE")||(mutant("M05")&&r.state.equals("CANCELLED")&&route.equals("cancel"))||(mutant("M07")&&r.state.equals("CANCELLED")&&route.equals("confirm"));
                if(!bad)throw new Fault(409,"INVALID_STATE","Invalid state transition");
                persist(obj("type","transition","id",r.id,"state",target));return new Reply(200,r.view());
            }
            case "advance":{
                fields(body,"millis");long delta=number(body.get("millis"),0,86400000);if(now>8000000000000000L-delta)throw invalid();
                long next=now+delta;persist(obj("type","clock","now",next));return new Reply(200,obj("now",now));
            }
            case "stats":{
                Map<String,Object> result=obj("items",(long)items.size(),"reservations",(long)reservations.size(),"active",0L,"confirmed",0L,"cancelled",0L,"expired",0L,"now",now);
                for(Reservation r:reservations.values()){String k=r.state.toLowerCase(Locale.ROOT);result.put(k,(Long)result.get(k)+1);}
                return new Reply(200,result);
            }
            case "list":{
                if(!Set.of("after","limit","state").containsAll(query.keySet()))throw invalid();
                long after=id(query.getOrDefault("after","0"),true);long limit=id(query.getOrDefault("limit","20"),false);if(limit>100)throw invalid();
                String state=query.get("state");if(state!=null&&!STATES.contains(state))throw invalid();List<Object> found=new ArrayList<>();
                for(Reservation r:reservations.tailMap(after,false).values()){
                    if(state!=null&&!r.state.equals(state)&&!mutant("M10"))continue;
                    found.add(r.view());if(found.size()>limit)break;
                }
                Object next=null;if(found.size()>limit){found.remove(found.size()-1);next=asMap(found.get(found.size()-1)).get("id");}
                return new Reply(200,obj("items",found,"nextAfter",next));
            }
            default:throw new Fault(404,"NOT_FOUND","Unknown route");
        }
    }
    @Override public synchronized void close()throws IOException{journal.close();}
}
