package org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers;

import org.jboss.byteman.agent.submit.ScriptText;

public interface LastScriptProvider {

    //todo void upload()

    ScriptText getLastScript();

    void setLastScript(ScriptText st);
}
