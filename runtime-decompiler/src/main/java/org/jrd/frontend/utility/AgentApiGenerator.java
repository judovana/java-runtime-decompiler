package org.jrd.frontend.utility;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.util.BytecodeExtractor;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.data.Cli;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;

import javax.swing.JComponent;
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
    private static final int MAX_NONLETERS_BETWEEN_MATCHES = 1;

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
        private final String haveThrows;
        protected final String masterPattern;

        protected ClazzMethod(String model) {
            masterPattern = model;
            haveThrows = model.trim().replace("java.lang.", "").replaceAll(".*\\)", "").replaceAll(";", "").replaceAll("\\$", ".")
                    .replace("org.jrd.agent.api.", "").trim();
            original = model.trim().replace("java.lang.", "").replace(PUBLIC_STATIC_PREFIX, "").replaceAll("\\).*", ")");
            resultType = original.trim().replaceAll(" .*", "");
            returnless = original.replaceFirst(resultType, "").trim();
        }

        @Override
        public String toString() {
            return original + " " + haveThrows;
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
                for (String clazz : new String[]{"org.jrd.agent.api.Variables", "org.jrd.agent.api.UnsafeVariables"}) {
                    String mainClazz = Cli.obtainClass(vmInfo, clazz, vmManager).getLoadedClassBytes();
                    if (withSignatures) {
                        Collection<ClazzMethod> mainMethods = getClazzMethods(vmInfo, vmManager, pluginManager, clazz, mainClazz);
                        agentApi.add(new ClazzWithMethods(clazz, mainMethods));
                    } else {
                        Collection<String> methods = BytecodeExtractor.extractMethods(Base64.getDecoder().decode(mainClazz));
                        agentApi.add(
                                new ClazzWithMethods(clazz, methods.stream().map(a -> new DummyClazzMethod(a)).collect(Collectors.toList()))
                        );
                    }
                    Set<String> innerClazzes = BytecodeExtractor.extractNestedClasses(
                            Base64.getDecoder().decode(mainClazz), new RuntimeCompilerConnector.JrdClassesProvider(vmInfo, vmManager)
                    );
                    for (String innerClazzName : innerClazzes) {
                        if (innerClazzName.matches(".*\\$[0-9]+$")) {
                            continue;
                        }
                        String innerClazz = Cli.obtainClass(vmInfo, innerClazzName, vmManager).getLoadedClassBytes();
                        if (withSignatures) {
                            Collection<ClazzMethod> methods = getClazzMethods(vmInfo, vmManager, pluginManager, innerClazzName, innerClazz);
                            agentApi.add(new ClazzWithMethods(innerClazzName, methods));
                        } else {
                            Collection<String> methods = BytecodeExtractor.extractMethods(Base64.getDecoder().decode(innerClazz));
                            agentApi.add(
                                    new ClazzWithMethods(
                                            innerClazzName, methods.stream().map(a -> new DummyClazzMethod(a)).collect(Collectors.toList())
                                    )
                            );
                        }
                    }
                    Collections.sort(agentApi, new Comparator<ClazzWithMethods>() {
                        @Override
                        public int compare(ClazzWithMethods clazzWithMethods, ClazzWithMethods t1) {
                            return t1.fqn.compareTo(clazzWithMethods.fqn);
                        }
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                agentApi = null;
            }
        }
    }

    private static Collection<ClazzMethod> getClazzMethods(
            VmInfo vmInfo, VmManager vmManager, PluginManager pluginManager, String innerClazzName, String innerClazz
    ) throws Exception {
        DecompilerWrapper decompiler = Cli.findDecompiler(DecompilerWrapper.JAVAP_NAME, pluginManager);
        String decompilationResult = pluginManager
                .decompile(decompiler, innerClazzName, Base64.getDecoder().decode(innerClazz), new String[0], vmInfo, vmManager);
        Collection<ClazzMethod> methods = extractMethods(decompilationResult);
        return methods;
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

    private static void insertOrRepalce(final RSyntaxTextArea text, final String nw, final String filter) {
        if (filter != null && !filter.trim().isEmpty()) {
            text.replaceRange(nw, text.getCaretPosition() - filter.length(), text.getCaretPosition());
        } else {
            text.insert(nw, text.getCaretPosition());
        }
    }

    public static JPopupMenu create(final RSyntaxTextArea text, String filter) {
        JPopupMenu p = new JPopupMenu();
        if (filter != null && !filter.trim().isEmpty()) {
            JMenuItem tool = new JMenuItem("Filtered by: " + filter);
            tool.setEnabled(false);
            p.add(tool);
        }
        add(filter, p, createExact("System.out.println(String);", text, filter), null);
        add(filter, p, createExact("System.err.println(String);", text, filter), null);
        if (agentApi == null) {
            JMenuItem item = new JMenuItem("Agent api completion still initialising. Try later");
            p.add(item);
            return p;
        }
        for (final ClazzWithMethods cwm : agentApi) {
            JMenuItem item = new JMenuItem(cwm.fqn);
            item.addActionListener(actionEvent -> {
                insertOrRepalce(text, cwm.fqn, filter);
                text.requestFocusInWindow();
            });
            add(filter, p, item, null);
            if (!cwm.methods.isEmpty()) {
                JMenu methods = new JMenu(cwm.fqn);
                for (final ClazzMethod methodName : cwm.methods) {
                    JMenuItem method = new JMenuItem(methodName.toString());
                    method.addActionListener(actionEvent -> {
                        insertOrRepalce(text, methodName.toOutput(cwm.fqn), filter);
                        text.requestFocusInWindow();
                    });
                    add(filter, methods, method, methodName.toOutput(cwm.fqn));
                }
                if (methods.getMenuComponentCount() > 0) {
                    p.add(methods);
                }
            }
        }

        p.addSeparator();
        p.add(createHelp(text));

        return p;
    }

    private static void add(String filter, JComponent p, JMenuItem toAdd, String additionalText) {
        if (additionalText == null) {
            additionalText = toAdd.getText();
        }
        if (containsAllInOrder(filter, additionalText)) {
            p.add(toAdd);
        }
    }

    static boolean containsAllInOrder(String filter, String text) {
        if (exitOnEmpty(filter, text) != null) {
            return exitOnEmpty(filter, text);
        }
        String pattern = filter.toLowerCase().trim();
        String where = text.toLowerCase().trim();
        if (where.contains(pattern)) {
            return true;
        }
        if (containsAll(pattern, where)) {
            return true;
        }
        int index1 = 0;
        int index2 = 0;
        int nonLeters = 0;
        while (true) {
            if (!(where.charAt(index2) + "").matches("[a-zA-Z]")) {
                nonLeters++;
            }
            if (pattern.charAt(index1) == where.charAt(index2)) {
                if (nonLeters > MAX_NONLETERS_BETWEEN_MATCHES) {
                    return false;
                }
                if (index1 == pattern.length() - 1) {
                    return true;
                }
                nonLeters = 0;
                index1++;
                index2++;
                if (index2 > where.length() - 1) {
                    return false;
                }
            } else {
                if (index2 == where.length() - 1) {
                    return false;
                }
                index2++;

            }
        }
    }

    private static boolean containsAll(String pattern, String where) {
        boolean foundAll = true;
        for (String word : pattern.split("[^a-zA-Z]+")) {
            if (!where.contains(word)) {
                foundAll = false;
            }
        }
        if (foundAll) {
            return true;
        }
        return false;
    }

    @SuppressFBWarnings(value = "NP_BOOLEAN_RETURN_NULL", justification = "optional is not nice. Intentional null boolean")
    private static Boolean exitOnEmpty(String filter, String text) {
        if (filter == null || filter.trim().isEmpty()) {
            return true;
        }
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return null;
    }

    private static JMenuItem createHelp(RSyntaxTextArea text) {
        JMenuItem i = new JMenuItem("Show help for this feature");
        i.addActionListener(actionEvent -> {
            JOptionPane.showMessageDialog(
                    text,
                    getPlainHelp() + "\n Use the API button or Ctrl+Space/Alt+Ins to bring up a selection of possible code insertions."
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

    private static JMenuItem createExact(String s, RSyntaxTextArea text, String filter) {
        JMenuItem i = new JMenuItem(s);
        i.addActionListener(actionEvent -> {
            insertOrRepalce(text, s + "\n", filter);
            text.requestFocusInWindow();
        });
        return i;
    }
}
