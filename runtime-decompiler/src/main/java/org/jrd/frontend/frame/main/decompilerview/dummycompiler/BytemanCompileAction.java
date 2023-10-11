package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;
import org.jboss.byteman.check.RuleCheck;
import org.jboss.byteman.check.RuleCheckResult;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.BytemanCompanion;
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

@SuppressWarnings("NestedIfDepth") // the unloadLastScript is moreover dead. probably best to drop it later
public class BytemanCompileAction extends AbstractCompileAction implements CanCompile {

    private final ClasspathProvider vmInfoProvider;
    //to unlaod last script may be awesome idea
    //but is very confusing for expereinced byteman users
    //better to provide unload all and unload this as separate buttons
    private final LastScriptProvider lastScriptProvider;

    @SuppressFBWarnings(
            value = "SS_SHOULD_BE_STATIC", justification = "unloadLastScript is placeholder for future " + "removal, unless found useful"
    )
    private final boolean unloadLastScript = false;
    private final UploadProvider boot;

    public String listAll() {
        try {
            return createSubmit().listAllRules();
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
            return ex.getMessage();
        }
    }

    /**
     * it is not list of lines, but list of scripts. So common input is list of size of 1
     *
     * @param scripts
     * @return
     */
    public String listSingleFile(List<String> scripts) {
        try {
            return scripts.stream().collect(Collectors.joining("\n\n"));
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
            return ex.getMessage();
        }
    }

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
            if (!results.hasError() || vmInfoProvider != null) {
                List<IdentifiedBytecode> r = new ArrayList<>();
                r.add(new IdentifiedBytecode(new ClassIdentifier("check.byteman"), script.getBytes(StandardCharsets.UTF_8)));
                if (vmInfoProvider == null) {
                    return r;
                } else {
                    Submit submit = createSubmit();
                    ScriptText st = new ScriptText("hi.btm", script);
                    if (lastScriptProvider.getLastScript() != null && unloadLastScript) {
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

    private Submit createSubmit() throws AgentLoadException, IOException, AttachNotSupportedException, AgentInitializationException {
        VmInfo vmInfo = vmInfoProvider.getVmInfo();
        BytemanCompanion bytemanCompanion = vmInfo.setBytemanCompanion(boot.isBoot(), vmInfo.getVmDecompilerStatus().getListenPort());
        Submit submit = new Submit(
                bytemanCompanion.getBytemanHost(), bytemanCompanion.getBytemanPort(),
                new PrintStream(new LogOutputStream(), true, StandardCharsets.UTF_8)
        );
        return submit;
    }

    @Override
    public DecompilerWrapper getWrapper() {
        return null;
    }

    public void removeAllRules() {
        try {
            String deleteAll = createSubmit().deleteAllRules();
            Logger.getLogger().log(Logger.Level.ALL, deleteAll);
            if (lastScriptProvider != null) {
                lastScriptProvider.setLastScript(null);
            }
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
        }
    }

    public void removeSingleRuleSet(String s) {
        try {
            ScriptText st = new ScriptText("by.btm", s);
            String deleteAll = createSubmit().deleteScripts(Collections.singletonList(st));
            Logger.getLogger().log(Logger.Level.ALL, deleteAll);
            if (lastScriptProvider != null) {
                lastScriptProvider.setLastScript(null);
            }
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
        }
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
            if (unloadLastScript) {
                if (lastScriptProvider != null) {
                    if (lastScriptProvider.getLastScript() == null) {
                        s = s + "<br/> nothing to unload";
                    } else {
                        s = s + "<br/> previous script will be unloaded";
                    }
                }
            }
        }
        if (boot != null) {
            s = s + "<br/> boot classlaoder = " + boot.isBoot();
        }
        return s;
    }

}
