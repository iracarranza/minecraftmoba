import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.BitSet;

/** Run with -javaagent and --add-opens java.base/java.util=ALL-UNNAMED. */
public final class MemoryProbe {
    private static Instrumentation instrumentation;
    public static void premain(String args, Instrumentation value) { instrumentation=value; }
    public static void main(String[] args) throws Exception {
        Field words=BitSet.class.getDeclaredField("words"); words.setAccessible(true);
        System.out.println("java="+System.getProperty("java.version")+" vm="+System.getProperty("java.vm.name"));
        System.out.println("payloadBytes,byteArrayShallowBytes,transientBitSetWithWordsBytes");
        for(String arg:args) {
            byte[] payload=new byte[Integer.parseInt(arg)];
            if(payload.length>0) payload[payload.length-1]=1;
            BitSet bits=BitSet.valueOf(payload);
            System.out.println(payload.length+","+instrumentation.getObjectSize(payload)+","+
                (instrumentation.getObjectSize(bits)+instrumentation.getObjectSize(words.get(bits))));
        }
    }
}
