package org.jrd.frontend.utility;

import io.github.mkoncek.classpathless.util.BytecodeExtractor;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.data.Cli;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class AgentApiGenerator {

    private static final class ClazzWithMethods {
        final String fqn;
        final List<String> methods;

        private ClazzWithMethods(String fqn, Collection<String> methods) {
            this.fqn = fqn.replace("$", ".");
            this.methods = methods.stream().filter(a -> !a.contains("init")).sorted().collect(Collectors.toList());
        }
    }

    private static List<ClazzWithMethods> agentApi;

    private AgentApiGenerator() {
    }

    public static synchronized void initItems(VmInfo vmInfo, VmManager vmManager) {
        if (agentApi == null) {
            try {
                agentApi = new ArrayList<>();
                String mainClazz = Cli.obtainClass(vmInfo, "org.jrd.agent.api.Variables", vmManager).getLoadedClassBytes();
                Set<String> innerClazzes = BytecodeExtractor.extractNestedClasses(
                        Base64.getDecoder().decode(mainClazz),
                        new RuntimeCompilerConnector.JrdClassesProvider(vmInfo, vmManager));
                for (String innerClazzName : innerClazzes) {
                    String innerClazz = Cli.obtainClass(vmInfo, innerClazzName, vmManager).getLoadedClassBytes();
                    Collection<String> methods = BytecodeExtractor.extractMethods(Base64.getDecoder().decode(innerClazz));
                    agentApi.add(new ClazzWithMethods(innerClazzName, methods));
                }
                Collections.sort(agentApi, new Comparator<ClazzWithMethods>() {
                    @Override
                    public int compare(ClazzWithMethods clazzWithMethods, ClazzWithMethods t1) {
                        return clazzWithMethods.fqn.compareTo(t1.fqn);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                agentApi = null;
            }
        }
    }


    public static JPopupMenu create(final RSyntaxTextArea text) {
        JPopupMenu p = new JPopupMenu();
        if (agentApi == null) {
            JMenuItem item = new JMenuItem("Agent api compltion still initialising. Try later");
            p.add(item);
            return p;
        }
        for (final ClazzWithMethods cwm : agentApi) {
            JMenuItem item = new JMenuItem(cwm.fqn);
            p.add(item);
            item.addActionListener(actionEvent -> {
                text.insert(cwm.fqn, text.getCaretPosition());
                text.requestFocusInWindow();
            });
            if (!cwm.methods.isEmpty()) {
                JMenu methods = new JMenu(cwm.fqn);
                for (final String methodName : cwm.methods) {
                    JMenuItem method = new JMenuItem(methodName);
                    methods.add(method);
                    method.addActionListener(actionEvent -> {
                        text.insert(cwm.fqn + "." + methodName + "();", text.getCaretPosition());
                        text.requestFocusInWindow();
                    });
                }
                p.add(methods);
            }
        }
        return p;
    }
}
