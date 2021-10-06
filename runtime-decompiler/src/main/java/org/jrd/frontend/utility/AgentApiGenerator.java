package org.jrd.frontend.utility;

import io.github.mkoncek.classpathless.util.BytecodeExtractor;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.data.Cli;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class AgentApiGenerator {

    public static final String PUBLIC_STATIC_PREFIX = "public static ";

    private static final class ClazzMethod implements Comparable<ClazzMethod> {
        private final String original;
        private final String resultType;
        private final String returnless;
        private final String masterPattern;

        private ClazzMethod(String model) {
            masterPattern = model;
            original = model.trim()
                    .replace("java.lang.", "")
                    .replace(PUBLIC_STATIC_PREFIX, "")
                    .replaceAll("\\).*", ")");
            resultType = original.trim().replaceAll(" .*", "");
            returnless = original.replaceFirst(resultType, "").trim();
        }

        @Override
        public String toString() {
            return original;
        }

        @Override
        public int compareTo(ClazzMethod clazzMethod) {
            return original.compareTo(clazzMethod.original);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ClazzMethod that = (ClazzMethod) o;
            return Objects.equals(original, that.original);
        }

        @Override
        public int hashCode() {
            return original.hashCode();
        }

        public String toOutput(String owner) {
            if ("void".equals(resultType)) {
                return owner + "." + returnless + ";\n";
            } else {
                return "(" + resultType + ")(" + owner + "." + returnless + ");\n";
            }
        }
    }

    private static final class ClazzWithMethods {
        final String fqn;
        final List<ClazzMethod> methods;

        private ClazzWithMethods(String fqn, Collection<ClazzMethod> methods) {
            this.fqn = fqn.replace("$", ".");
            this.methods = methods.stream().filter(a -> !a.toString().contains("init")).sorted().collect(Collectors.toList());
        }
    }

    private static List<ClazzWithMethods> agentApi;

    private AgentApiGenerator() {
    }

    public static synchronized void initItems(VmInfo vmInfo, VmManager vmManager, PluginManager pluginManager) {
        if (agentApi == null) {
            try {
                agentApi = new ArrayList<>();
                String mainClazz = Cli.obtainClass(vmInfo, "org.jrd.agent.api.Variables", vmManager).getLoadedClassBytes();
                Set<String> innerClazzes = BytecodeExtractor.extractNestedClasses(
                        Base64.getDecoder().decode(mainClazz),
                        new RuntimeCompilerConnector.JrdClassesProvider(vmInfo, vmManager));
                for (String innerClazzName : innerClazzes) {
                    String innerClazz = Cli.obtainClass(vmInfo, innerClazzName, vmManager).getLoadedClassBytes();
                    DecompilerWrapper decompiler = Cli.findDecompiler(DecompilerWrapper.JAVAP_NAME, pluginManager);
                    String decompilationResult = pluginManager.decompile(
                            decompiler,
                            innerClazzName,
                            Base64.getDecoder().decode(innerClazz),
                            new String[0], vmInfo, vmManager);
                    Collection<ClazzMethod> methods = extractMethods(decompilationResult);
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

    private static Collection<ClazzMethod> extractMethods(String decompilationResult) {
        Collection<ClazzMethod> methods = new ArrayList<>(0);
        String[] lines = decompilationResult.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith(PUBLIC_STATIC_PREFIX) && !line.contains("{")) {
                methods.add(new ClazzMethod(line));
            }
        }
        return methods;
    }


    public static JPopupMenu create(final RSyntaxTextArea text) {
        JPopupMenu p = new JPopupMenu();
        p.add(createExact("System.out.println(String);", text));
        p.add(createExact("System.err.println(String);", text));
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
                for (final ClazzMethod methodName : cwm.methods) {
                    JMenuItem method = new JMenuItem(methodName.toString());
                    methods.add(method);
                    method.addActionListener(actionEvent -> {
                        text.insert(methodName.toOutput(cwm.fqn), text.getCaretPosition());
                        text.requestFocusInWindow();
                    });
                }
                p.add(methods);
            }
        }
        p.add(createHelp(text));
        return p;
    }

    private static JMenuItem createHelp(RSyntaxTextArea text) {
        JMenuItem i = new JMenuItem("Help");
        i.addActionListener(actionEvent -> {
            JOptionPane.showMessageDialog(text,
                    "This api allows you to \"insert fields\" and \"declare\" new methods (aka Runnable)\n" +
                            "into *running vm*. This api have no sense in filesystem jar/classes. To read more,\n" +
                            "try to decompile classes from package: \'org.jrd.agent.api\' to see full api and logic");
        });
        return i;
    }

    private static JMenuItem createExact(String s, RSyntaxTextArea text) {
        JMenuItem i = new JMenuItem(s);
        i.addActionListener(actionEvent -> {
            text.insert(s + "\n", text.getCaretPosition());
            text.requestFocusInWindow();
        });
        return i;
    }
}
