package benchkit;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Supplied append-only primitive. Each frame is length:int32, crc32:int32, UTF-8 JSON.
 * Partial last frames are truncated; complete corrupt frames fail closed.
 * The candidate decides event schema, replay, transaction boundaries, and memory ordering.
 */
public final class Journal implements AutoCloseable {
    private static final int MAX_FRAME=1_048_576;
    private final FileChannel channel, lockChannel;
    private final FileLock lock;
    private final List<Object> history=new ArrayList<>();
    private long attempts;
    private final long failAt;
    public Journal(Path dir) throws IOException {
        Files.createDirectories(dir);
        lockChannel=FileChannel.open(dir.resolve(".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        FileLock acquired;
        try{acquired=lockChannel.tryLock();}catch(RuntimeException e){lockChannel.close();throw new IOException("Data directory is locked",e);}
        if(acquired==null){lockChannel.close();throw new IOException("Data directory is locked");}
        lock=acquired;
        FileChannel opened=null;
        try{
            opened=FileChannel.open(dir.resolve("events.log"),StandardOpenOption.CREATE,StandardOpenOption.READ,StandardOpenOption.WRITE);
            channel=opened;
            String v=System.getenv("BENCH_FAIL_APPEND_AT");failAt=v==null?0:Long.parseLong(v);
            readFrames();
        }catch(Exception e){if(opened!=null)opened.close();lock.release();lockChannel.close();if(e instanceof IOException)throw (IOException)e;throw new IOException("Cannot open journal",e);}
    }
    public List<Object> records(){return Collections.unmodifiableList(history);}
    private void readFrames() throws IOException {
        long good=0,size=channel.size();
        while(good<size){
            if(size-good<8){truncate(good);break;}
            channel.position(good);ByteBuffer h=ByteBuffer.allocate(8);readFully(h);h.flip();int n=h.getInt();int expected=h.getInt();
            if(n<=0||n>MAX_FRAME)throw new IOException("Corrupt journal frame length");
            if(size-good-8<n){truncate(good);break;}
            ByteBuffer p=ByteBuffer.allocate(n);readFully(p);byte[] bytes=p.array();CRC32 crc=new CRC32();crc.update(bytes);
            if((int)crc.getValue()!=expected)throw new IOException("Corrupt journal checksum");
            try{
                String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
                history.add(Json.parse(text));
            }catch(Exception e){throw new IOException("Corrupt journal JSON",e);}
            good+=8L+n;
        }
        channel.position(channel.size());
    }
    private void readFully(ByteBuffer b)throws IOException{while(b.hasRemaining()){if(channel.read(b)<0)throw new EOFException();}}
    private void truncate(long pos)throws IOException{channel.truncate(pos);channel.force(true);}
    public synchronized void append(Object event)throws IOException{
        attempts++;
        if(failAt>0&&attempts==failAt)throw new IOException("Injected append failure");
        byte[] bytes=Json.stringify(event).getBytes(StandardCharsets.UTF_8);
        if(bytes.length==0||bytes.length>MAX_FRAME)throw new IOException("Journal frame too large");
        CRC32 crc=new CRC32();crc.update(bytes);ByteBuffer b=ByteBuffer.allocate(8+bytes.length);b.putInt(bytes.length).putInt((int)crc.getValue()).put(bytes).flip();
        long start=channel.size();
        try{channel.position(start);while(b.hasRemaining())channel.write(b);channel.force(true);}
        catch(IOException e){try{truncate(start);channel.position(start);}catch(IOException rollback){e.addSuppressed(rollback);}throw e;}
    }
    @Override public synchronized void close()throws IOException{
        try{channel.close();}finally{try{lock.release();}finally{lockChannel.close();}}
    }
}
