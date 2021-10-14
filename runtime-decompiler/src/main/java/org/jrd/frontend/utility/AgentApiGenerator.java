package org.jrd.frontend.utility;

import io.github.mkoncek.classpathless.util.BytecodeExtractor;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.data.Cli;
import org.jrd.backend.data.Config;
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

    private static final class DummyClazzMethod extends ClazzMethod {
        private DummyClazzMethod(String method) {
            super(method);
        }

        @Override
        public String toString() {
            return masterPattern;
        }

        @Override
        public String toOutput(String owner) {
            return "(Object)(" + owner + "." + masterPattern + ")\n";
        }
    }

    private static class ClazzMethod implements Comparable<ClazzMethod> {
        private final String original;
        private final String resultType;
        private final String returnless;
        protected final String masterPattern;

        protected ClazzMethod(String model) {
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
            return toString().compareTo(clazzMethod.toString());
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
            return Objects.equals(toString(), that.toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
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
        initItems(vmInfo, vmManager, pluginManager, Config.getConfig().doUseJavapSignatures());
    }

    private static synchronized void initItems(VmInfo vmInfo, VmManager vmManager, PluginManager pluginManager, boolean withSignatures) {
        if (agentApi == null) {
            try {
                agentApi = new ArrayList<>();
                String mainClazz = Cli.obtainClass(vmInfo, "org.jrd.agent.api.Variables", vmManager).getLoadedClassBytes();
                Set<String> innerClazzes = BytecodeExtractor.extractNestedClasses(
                        Base64.getDecoder().decode(mainClazz),
                        new RuntimeCompilerConnector.JrdClassesProvider(vmInfo, vmManager));
                for (String innerClazzName : innerClazzes) {
                    String innerClazz = Cli.obtainClass(vmInfo, innerClazzName, vmManager).getLoadedClassBytes();
                    if (withSignatures) {
                        DecompilerWrapper decompiler = Cli.findDecompiler(DecompilerWrapper.JAVAP_NAME, pluginManager);
                        String decompilationResult = pluginManager.decompile(
                                decompiler,
                                innerClazzName,
                                Base64.getDecoder().decode(innerClazz),
                                new String[0], vmInfo, vmManager);
                        Collection<ClazzMethod> methods = extractMethods(decompilationResult);
                        agentApi.add(new ClazzWithMethods(innerClazzName, methods));
                    } else {
                        Collection<String> methods = BytecodeExtractor.extractMethods(Base64.getDecoder().decode(innerClazz));
                        agentApi.add(new ClazzWithMethods(innerClazzName,
                                methods.stream().map(a -> new DummyClazzMethod(a)).collect(Collectors.toList())));
                    }
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

    /**
     * Used to clear Agent API items in case their method/form of generation changes.
     */
    public static synchronized void clearItems() {
        agentApi = null;
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
            JMenuItem item = new JMenuItem("Agent api completion still initialising. Try later");
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

        p.addSeparator();
        p.add(createHelp(text));

        return p;
    }

    private static JMenuItem createHelp(RSyntaxTextArea text) {
        JMenuItem i = new JMenuItem("Show help for this feature");
        i.addActionListener(actionEvent -> {
            JOptionPane.showMessageDialog(
                text,
                getPlainHelp() + "\n Use the API button or Ctrl+Space to bring up a selection of possible code insertions."
            );
        });
        return i;
    }

    public static String getPlainHelp() {
        return "This API allows you to insert new fields and declare new methods (Runnables) in a running JVM.\n" +
            "To read more, decompile classes from package 'org.jrd.agent.api' to see the full source code and logic.";
    }

    public static String getInterestingHelp() {
        StringBuilder sb = new StringBuilder(getPlainHelp());
        sb.append("\n");
        sb.append("\n");
        if (agentApi == null) {
            sb.append("Agent api help generation failed");
            return sb.toString();
        }
        for (final ClazzWithMethods cwm : agentApi) {
            sb.append(cwm.fqn).append("\n");
            if (!cwm.methods.isEmpty()) {
                for (final ClazzMethod methodName : cwm.methods) {
                    sb.append("    ").append(methodName.toString()).append("\n");
                    sb.append("        ").append(methodName.toOutput(cwm.fqn).trim()).append("\n");
                }
            }
        }
        return sb.toString();
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
