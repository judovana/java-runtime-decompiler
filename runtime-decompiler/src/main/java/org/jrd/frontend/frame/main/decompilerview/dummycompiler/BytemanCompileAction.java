package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import org.jboss.byteman.agent.install.Install;
import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;
import org.jboss.byteman.check.RuleCheck;
import org.jboss.byteman.check.RuleCheckResult;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.ClasspathProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.LastScriptProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.UploadProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class BytemanCompileAction extends AbstractCompileAction implements CanCompile {

    private final ClasspathProvider vmInfoProvider;
    private final LastScriptProvider lastScriptProvider;
    private final UploadProvider boot;

    @Override
    public Collection<IdentifiedBytecode> compile(List<String> scripts, PluginManager pluginManager) {
        String script = scripts.stream().collect(Collectors.joining("\n\n"));
        RuleCheck check = new RuleCheck();
        if (Logger.getLogger().isVerbose()) {
            check.setVerbose();
        }
        try {
            check.setPrintStream(new PrintStream(new LogOutputStream(), true, StandardCharsets.UTF_8));
            check.addRule(new Date().toString(), script);
            check.checkRules();
            RuleCheckResult results = check.getResult();
            if (!results.hasError()) {
                List<IdentifiedBytecode> r = new ArrayList<>();
                r.add(new IdentifiedBytecode(new ClassIdentifier("check.byteman"), script.getBytes(StandardCharsets.UTF_8)));
                if (vmInfoProvider == null) {
                    return r;
                } else {
                    VmInfo vmInfo = vmInfoProvider.getVmInfo();
                    int pid = vmInfo.getVmPid();
                    int port;
                    if (vmInfo.getBytemanCompanion() != null) {
                        port = vmInfo.getBytemanCompanion();
                    } else {
                        port = VmInfo.findFreePort();
                        Install.install("" + pid, boot.isBoot(), "localhost", port, new String[]{});
                        vmInfo.setBytemanCompanion(port);
                    }
                    Submit submit = new Submit("localhost", port, new PrintStream(new LogOutputStream(), true, StandardCharsets.UTF_8));
                    ScriptText st = new ScriptText("hi.btm", script);
                    if (lastScriptProvider.getLastScript() != null) {
                        String deleteAll = submit.deleteScripts(Collections.singletonList(lastScriptProvider.getLastScript()));
                        Logger.getLogger().log(Logger.Level.ALL, deleteAll);
                        lastScriptProvider.setLastScript(null);
                    }
                    String add = submit.addScripts(Collections.singletonList(st));
                    lastScriptProvider.setLastScript(st);
                    Logger.getLogger().log(Logger.Level.ALL, add);
                    return r;
                }
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

    public BytemanCompileAction(
            String title, ClasspathProvider vmInfoProvider, LastScriptProvider lastScriptProvider, UploadProvider boot
    ) {
        super(title);
        this.vmInfoProvider = vmInfoProvider;
        this.lastScriptProvider = lastScriptProvider;
        this.boot = boot;
    }

    @Override
    public String getText() {
        String s = super.getText();
        if (vmInfoProvider != null) {
            s = s + "<br/> will be uploaded installed to:" + vmInfoProvider.getClasspath().cpTextInfo();
            if (lastScriptProvider != null) {
                if (lastScriptProvider.getLastScript() == null) {
                    s = s + "<br/> nothing to unload";
                } else {
                    s = s + "<br/> previous script will be unloaded";
                }
            }
        }
        if (boot != null) {
            s = s + "<br/> boot classlaoder = " + boot.isBoot();
        }
        return s;
    }

}
