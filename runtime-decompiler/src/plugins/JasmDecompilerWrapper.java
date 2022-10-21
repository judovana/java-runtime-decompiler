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

import org.openjdk.asmtools.common.ToolInput;
import org.openjdk.asmtools.common.ToolOutput;

public class JasmDecompilerWrapper {

    public String decompile(byte[] bytecode, String[] options) {
        try {
            log(null, "jasm decompiler caled with input of bytes: " + bytecode.length);
            ToolInput[] originalFiles = new ToolInput[]{new ToolInput.ByteInput(bytecode)};
            ToolOutput.TextOutput decodedFiles = new ToolOutput.TextOutput();
            ToolOutput.SingleDualOutputStreamOutput decodeLog = new ToolOutput.SingleDualOutputStreamOutput();
            org.openjdk.asmtools.jdis.Main jdis = new org.openjdk.asmtools.jdis.Main(decodedFiles, decodeLog, originalFiles);
            jdis.setVerboseFlag(true);
            int r = jdis.disasm();
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
        log(maybeLogger, "jasm compiler caled with input of files: " + src.size());
        ToolInput[] originalFiles = new ToolInput[src.size()];
        ArrayList<Map.Entry<String,String>> input = new ArrayList<>(src.entrySet());
        for (int i = 0; i < input.size(); i++) {
            originalFiles[i] = new ToolInput.ByteInput(input.get(i).getValue());
        }
        ToolOutput.ByteOutput encodedFiles = new ToolOutput.ByteOutput();
        ToolOutput.SingleDualOutputStreamOutput encodeLog = new ToolOutput.SingleDualOutputStreamOutput();
        org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(encodedFiles, encodeLog, originalFiles);
        jasm.setVerboseFlag(true);
        int r = jasm.compile();
        Map<String, byte[]> results = new HashMap<>(src.size());
        for(ToolOutput.ByteOutput.NamedBinary nb: encodedFiles.getOutputs()) {
            results.put(nb.getFqn().replace("/", "."), nb.getBody());
        }
        return results;
    }
}
