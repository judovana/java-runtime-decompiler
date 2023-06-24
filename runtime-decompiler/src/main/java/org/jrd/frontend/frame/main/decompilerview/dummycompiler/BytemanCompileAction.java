package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import org.jboss.byteman.check.RuleCheck;
import org.jboss.byteman.check.RuleCheckResult;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class BytemanCompileAction extends AbstractCompileAction implements CanCompile {

    private final VmInfo vmInfo;
    private String lastCopyToUnload;

    @Override
    public Collection<IdentifiedBytecode> compile(String s, PluginManager pluginManager, String execute) {
        //org.jboss.byteman.check.TestScript.main();
        lastCopyToUnload = s; //FIXME place , usage and when...onlyh if uploading.. so if execute is not n null
        RuleCheck check = new RuleCheck();
        if (Logger.getLogger().isVerbose()) {
            check.setVerbose();
        }
        try {
            check.setPrintStream(new PrintStream(new LogOutputStream(), true, StandardCharsets.UTF_8));
            check.addRule(new Date().toString(), s);
            check.checkRules();
            RuleCheckResult results = check.getResult();
            if (!results.hasError()) {
                List<IdentifiedBytecode> r = new ArrayList<>();
                r.add(new IdentifiedBytecode(new ClassIdentifier("check.byteman"), s.getBytes(StandardCharsets.UTF_8)));
                return r;
            }
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
        }
        return null;
    }

    @Override
    public DecompilerWrapper getWrapper() {
        return null;
    }

    public static class LogOutputStream extends OutputStream {

        private StringBuilder sb = new StringBuilder();

        @Override
        public void write(int b) throws IOException {
            if (b == '\n') {
                Logger.getLogger().log(Logger.Level.ALL, sb.toString());
                sb.setLength(0);
            } else {
                sb.append((char) b);
            }
        }

    }

    public BytemanCompileAction(String title, VmInfo vmInfo) {
        super(title);
        this.vmInfo = vmInfo;
    }

}
