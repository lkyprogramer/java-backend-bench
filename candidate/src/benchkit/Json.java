package benchkit;

import java.math.BigDecimal;
import java.util.*;

/** Supplied, frozen JSON codec. Domain validation is the candidate's responsibility. */
public final class Json {
    private Json() {}
    public static Object parse(String text) {
        Parser p = new Parser(text);
        Object value = p.value(0);
        p.ws();
        if (p.i != text.length()) throw new IllegalArgumentException("Trailing JSON data");
        return value;
    }
    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value,out,0);
        return out.toString();
    }
    private static void write(Object x,StringBuilder b,int depth) {
        if(depth>64) throw new IllegalArgumentException("JSON nesting limit");
        if(x==null){b.append("null");return;}
        if(x instanceof String){quote((String)x,b);return;}
        if(x instanceof Boolean || x instanceof Number){
            String s=x.toString();
            if(s.equals("NaN")||s.contains("Infinity")) throw new IllegalArgumentException("Non-finite number");
            b.append(s);return;
        }
        if(x instanceof Map){
            b.append('{');boolean comma=false;
            for(Object e0:((Map<?,?>)x).entrySet()){
                Map.Entry<?,?> e=(Map.Entry<?,?>)e0;
                if(!(e.getKey() instanceof String))throw new IllegalArgumentException("Non-string key");
                if(comma)b.append(',');comma=true;quote((String)e.getKey(),b);b.append(':');write(e.getValue(),b,depth+1);
            }b.append('}');return;
        }
        if(x instanceof Iterable){
            b.append('[');boolean comma=false;
            for(Object v:(Iterable<?>)x){if(comma)b.append(',');comma=true;write(v,b,depth+1);}b.append(']');return;
        }
        throw new IllegalArgumentException("Unsupported JSON type");
    }
    private static void quote(String s,StringBuilder b){
        b.append('"');
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            switch(c){
                case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;
                case '\b':b.append("\\b");break;case '\f':b.append("\\f");break;
                case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;
                default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);
            }
        }b.append('"');
    }
    private static final class Parser{
        final String s;int i;
        Parser(String s){this.s=Objects.requireNonNull(s);}
        void ws(){while(i<s.length() && " \n\r\t".indexOf(s.charAt(i))>=0)i++;}
        IllegalArgumentException bad(){return new IllegalArgumentException("Invalid JSON at "+i);}
        char take(){if(i>=s.length())throw bad();return s.charAt(i++);}
        boolean eat(char c){ws();if(i<s.length()&&s.charAt(i)==c){i++;return true;}return false;}
        Object value(int d){
            if(d>64)throw bad();ws();if(i>=s.length())throw bad();char c=s.charAt(i);
            if(c=='"')return string();
            if(c=='{'){
                i++;Map<String,Object> m=new LinkedHashMap<>();if(eat('}'))return m;
                do{ws();if(i>=s.length()||s.charAt(i)!='"')throw bad();String k=string();if(m.containsKey(k)||!eat(':'))throw bad();m.put(k,value(d+1));}while(eat(','));
                if(!eat('}'))throw bad();return m;
            }
            if(c=='['){i++;List<Object>a=new ArrayList<>();if(eat(']'))return a;do{a.add(value(d+1));}while(eat(','));if(!eat(']'))throw bad();return a;}
            if(s.startsWith("true",i)){i+=4;return true;}if(s.startsWith("false",i)){i+=5;return false;}if(s.startsWith("null",i)){i+=4;return null;}
            int start=i;if(c=='-')i++;
            if(i>=s.length())throw bad();
            if(s.charAt(i)=='0')i++;else{if(s.charAt(i)<'1'||s.charAt(i)>'9')throw bad();while(i<s.length()&&(s.charAt(i)>='0'&&s.charAt(i)<='9'))i++;}
            boolean decimal=false;
            if(i<s.length()&&s.charAt(i)=='.'){decimal=true;i++;int a=i;while(i<s.length()&&(s.charAt(i)>='0'&&s.charAt(i)<='9'))i++;if(a==i)throw bad();}
            if(i<s.length()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){decimal=true;i++;if(i<s.length()&&(s.charAt(i)=='+'||s.charAt(i)=='-'))i++;int a=i;while(i<s.length()&&(s.charAt(i)>='0'&&s.charAt(i)<='9'))i++;if(a==i)throw bad();}
            try{String n=s.substring(start,i);if(decimal)return new BigDecimal(n);return Long.valueOf(n);}catch(NumberFormatException e){throw bad();}
        }
        String string(){
            if(take()!='"')throw bad();StringBuilder b=new StringBuilder();
            while(true){char c=take();if(c=='"')return b.toString();if(c<32)throw bad();
                if(c=='\\'){c=take();switch(c){
                    case '"':case '\\':case '/':b.append(c);break;
                    case 'b':b.append('\b');break;case 'f':b.append('\f');break;case 'n':b.append('\n');break;case 'r':b.append('\r');break;case 't':b.append('\t');break;
                    case 'u':int code=0;for(int j=0;j<4;j++){char h=take();int v=h>='0'&&h<='9'?h-'0':h>='a'&&h<='f'?h-'a'+10:h>='A'&&h<='F'?h-'A'+10:-1;if(v<0)throw bad();code=code*16+v;}b.append((char)code);break;
                    default:throw bad();
                }}else b.append(c);
            }
        }
    }
}
