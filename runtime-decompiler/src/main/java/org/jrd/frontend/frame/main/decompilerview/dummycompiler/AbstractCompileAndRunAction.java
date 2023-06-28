package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.ClasspathProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.ExecuteMethodProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.SaveProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.UploadProvider;

public abstract class AbstractCompileAndRunAction extends AbstractCompileAction {

    protected final ClasspathProvider classesAndMethodsProvider;
    protected final SaveProvider save;
    protected final UploadProvider upload;
    protected final ExecuteMethodProvider execute;

    public AbstractCompileAndRunAction(
            String stub, ClasspathProvider classesAndMethodsProvider, SaveProvider save, UploadProvider upload,
            ExecuteMethodProvider execute
    ) {
        super(stub);
        this.classesAndMethodsProvider = classesAndMethodsProvider;
        this.save = save;
        this.upload = upload;
        this.execute = execute;
    }

    @Override
    public String getText() {
        String s = super.getText();
        if (classesAndMethodsProvider != null) {
            s = s + "<br/>" + classesAndMethodsProvider.getClasspath().cpTextInfo() + "<br/>";
        } else {
            s = s + "<br/>";
        }
        if (save != null) {
            if (save.getSaveDirectory() == null) {
                s = s + "; no saving";
            } else {
                s = s + "; save to: " + save.getSaveDirectory().getAbsolutePath();
            }
        }
        if (execute != null) {
            if (execute.getMethodToExecute() == null) {
                s = s + "; nothing to execute";
            } else {
                s = s + "; will be executed: `" + execute.getMethodToExecute() + "`";
            }
        }
        if (upload != null) {
            if (!upload.isUploadEnabled()) {
                s = s + "; no adding to running vm";
            } else {
                s = s + "; <b>will be added to:</b> " + upload.getTarget().getClasspath().cpTextInfo();
                if (upload.isBoot()) {
                    s = s + " to Boot cp!!!";
                }
            }
        }
        return s;
    }
}
