import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;
import java.util.Set;

import org.openjdk.asmtools.common.inputs.*;
import org.openjdk.asmtools.common.outputs.log.*;
import org.openjdk.asmtools.common.outputs.*;

public class JcoderDecompilerWrapper {

    public String decompile(byte[] bytecode, String[] options) {
        try {
            log(null, "jcoder decompiler caled with input of bytes: " + bytecode.length);
            ToolInput[] originalFiles = new ToolInput[]{new ByteInput(bytecode)};
            TextOutput decodedFiles = new TextOutput();
            SingleDualOutputStreamOutput decodeLog = new StderrLog();
            org.openjdk.asmtools.jdec.Main jdec = new org.openjdk.asmtools.jdec.Main(decodedFiles, decodeLog, originalFiles);
            jdec.setVerboseFlag(true);
            int r = jdec.decode();
            return decodedFiles.getOutputs().get(0).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    private void log(Object logger, String message) {
        System.err.println(message);
    }

    public Map<String, byte[]> compile(Map<String, String> src, String[] options, Object maybeLogger) throws Exception {
        log(maybeLogger, "jcoder compiler caled with input of files: " + src.size());
        ToolInput[] originalFiles = new ToolInput[src.size()];
        ArrayList<Map.Entry<String,String>> input = new ArrayList<>(src.entrySet());
        for (int i = 0; i < input.size(); i++) {
            originalFiles[i] = new StringInput(input.get(i).getValue());
        }
        ByteOutput encodedFiles = new ByteOutput();
        SingleDualOutputStreamOutput encodeLog = new StderrLog();
        org.openjdk.asmtools.jcoder.Main jcoder = new org.openjdk.asmtools.jcoder.Main(encodedFiles, encodeLog, originalFiles);
        jcoder.setVerboseFlag(true);
        int r = jcoder.compile();
        Map<String, byte[]> results = new HashMap<>(src.size());
        for(ByteOutput.NamedBinary nb: encodedFiles.getOutputs()) {
            results.put(nb.getFqn().replace("/", ".").replaceAll("\\.class$",""), nb.getBody());
        }
        return results;
    }
}
