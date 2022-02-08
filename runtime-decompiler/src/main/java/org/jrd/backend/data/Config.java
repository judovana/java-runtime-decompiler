package org.jrd.backend.data;

import com.google.gson.Gson;
import org.jrd.backend.core.Logger;
import org.jrd.backend.decompiling.ExpandableUrl;
import org.jrd.frontend.utility.AgentApiGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Singleton class for storing and retrieving configuration strings.
 */
public final class Config {

    private final Gson gson;
    private Map<String, Object> configMap;

    private static final String CONFIG_PATH = Directories.getConfigDirectory() + File.separator + "config.json";
    private static final String LEGACY_CONFIG_PATH = Directories.getConfigDirectory() + File.separator + "config.cfg";

    public static final String AGENT_PATH_OVERWRITE_PROPERTY = "org.jrd.agent.jar";
    private static final String AGENT_PATH_KEY = "AGENT_PATH";
    private static final String SAVED_FS_VMS_KEY = "FS_VMS";
    private static final String USE_HOST_SYSTEM_CLASSES_KEY = "USE_HOST_SYSTEM_CLASSES";
    private static final String USE_HOST_JAVA_LANG_OBJECT = "USE_HOST_JAVA_LANG_OBJECT";
    private static final String NESTED_JAR_EXTENSIONS = "NESTED_JAR_EXTENSIONS";
    private static final String COMPILER_ARGS = "COMPILER_ARGS";
    private static final String USE_JAVAP_SIGNATURES = "USE_JAVAP_SIGNATURES";

    public enum DepndenceNumbers {
        ENFORCE_ONE("This will pass only selected class to decompiler. Fastest, worst results, may have its weird usecase"),
        ALL_INNERS("Together with selected class, also all its inner classes are send to decompiler. Fast. Good enough results"),
        ALL(
                "Together with selected class, also all classes it depends on  are send to decompiler." +
                        " Slow, best results. Also it forces java.* internal classes from host, not local"
        );

        private final String description;

        DepndenceNumbers(String s) {
            description = s;
        }
    }

    @SuppressFBWarnings(value = {"DMI_RANDOM_USED_ONLY_ONCE"}, justification = "will be gone when properly implemented")
    public DepndenceNumbers getDepndenciesNumber() {
        int it = new Random().nextInt(3);
        if (it == 0) {
            System.err.println("ALL");
            return DepndenceNumbers.ALL;
        } else if (it == 1) {
            System.err.println("ALL_INNERS");
            return DepndenceNumbers.ALL_INNERS;
        } else {
            System.err.println("ENFORCE_ONE");
            return DepndenceNumbers.ENFORCE_ONE;
        }
    }

    private static class ConfigHolder {
        private static final Config INSTANCE = new Config();
    }

    private Config() {
        gson = new Gson();

        try {
            loadConfigFile();
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.ALL, e);
        }
    }

    public static Config getConfig() {
        return ConfigHolder.INSTANCE;
    }

    private ExpandableUrl createAgentExpandableUrl() {
        // overrule everything else with property
        String propertyAgentPath = System.getProperty(AGENT_PATH_OVERWRITE_PROPERTY);
        if (propertyAgentPath != null && !propertyAgentPath.isEmpty()) {
            return ExpandableUrl.createFromPath(propertyAgentPath);
        }

        String configAgentPath = (String) configMap.getOrDefault(AGENT_PATH_KEY, "");
        String potentialAgentPath = Directories.getPotentialAgentLocation().getAbsolutePath();

        // if config doesn't contain agent path, but agent jar is in a predictable place, use that
        if (configAgentPath.isEmpty() && new File(potentialAgentPath).exists()) {
            configAgentPath = potentialAgentPath;

            configMap.put(AGENT_PATH_KEY, potentialAgentPath);
            try {
                saveConfigFile();
            } catch (IOException e) {
                Logger.getLogger().log(Logger.Level.ALL, e);
            }
        }

        return ExpandableUrl.createFromPath(configAgentPath);
    }

    public String getAgentRawPath() {
        return createAgentExpandableUrl().getRawPath();
    }

    public String getAgentExpandedPath() {
        String expandedPath = createAgentExpandableUrl().getExpandedPath();

        // Agent attaching fails on Windows when path starts with a slash
        if (Directories.isOsWindows() && expandedPath.length() > 0 && expandedPath.charAt(0) == '/') {
            expandedPath = expandedPath.substring(1);
        }

        return expandedPath;
    }

    public void setAgentPath(String agentPath) {
        if (agentPath.endsWith(".jar")) {
            configMap.put(AGENT_PATH_KEY, ExpandableUrl.createFromPath(agentPath).getRawPath());
        } else {
            Logger.getLogger().log(Logger.Level.ALL, new RuntimeException("Agent must be a .jar file"));
        }
    }

    @SuppressWarnings("unchecked") // gson reads JSON arrays as List<String>
    private List<String> getOrCreateSavedFsVms() {
        List<String> savedFsVms = (List<String>) configMap.get(SAVED_FS_VMS_KEY);

        if (savedFsVms == null) {
            savedFsVms = new ArrayList<>();
            configMap.put(SAVED_FS_VMS_KEY, savedFsVms);
        }

        return savedFsVms;
    }

    public List<VmInfo> getSavedFsVms() throws IOException, ClassNotFoundException {
        List<VmInfo> result = new ArrayList<>();

        for (String base64String : getOrCreateSavedFsVms()) {
            result.add(VmInfo.base64Deserialize(base64String));
        }

        return result;
    }

    public void addSavedFsVm(VmInfo vmInfo) throws IOException {
        getOrCreateSavedFsVms().add(vmInfo.base64Serialize());
    }

    public void setUseHostSystemClasses(boolean useHostJavaClasses) {
        configMap.put(USE_HOST_SYSTEM_CLASSES_KEY, useHostJavaClasses);
    }

    public void setUseHostJavaLangObject(boolean useHostJavaLangObject) {
        configMap.put(USE_HOST_JAVA_LANG_OBJECT, useHostJavaLangObject);
    }

    public boolean doUseHostSystemClasses() {
        return (boolean) configMap.getOrDefault(USE_HOST_SYSTEM_CLASSES_KEY, true);
    }

    public boolean doUseHostJavaLangObject() {
        return (boolean) configMap.getOrDefault(USE_HOST_JAVA_LANG_OBJECT, true);
    }

    public void setNestedJarExtensions(List<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return;
        }

        configMap.put(NESTED_JAR_EXTENSIONS, extensions);
    }

    @SuppressWarnings("unchecked") // gson reads JSON arrays as List<String>
    public List<String> getNestedJarExtensions() {
        List<String> savedExtensions = (List<String>) configMap.get(NESTED_JAR_EXTENSIONS);

        if (savedExtensions == null) {
            return new ArrayList<>();
        }

        return Collections.unmodifiableList(savedExtensions);
    }

    public String getCompilerArgsString() {
        return String.join(" ", getCompilerArgs());
    }

    @SuppressWarnings("unchecked") // gson reads JSON arrays as List<String>
    public List<String> getCompilerArgs() {
        List<String> savedCompilerArgs = (List<String>) configMap.get(COMPILER_ARGS);

        if (savedCompilerArgs == null || savedCompilerArgs.size() == 1 && savedCompilerArgs.get(0).isBlank()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(savedCompilerArgs);
    }

    public void setCompilerArguments(String argString) {
        List<String> argList;

        if (argString.isBlank()) {
            argList = new ArrayList<>();
        } else {
            argList = Arrays.asList(argString.split("\\s+"));
        }

        configMap.put(COMPILER_ARGS, argList);
    }

    public boolean doUseJavapSignatures() {
        return (boolean) configMap.getOrDefault(USE_JAVAP_SIGNATURES, true);
    }

    public void setUseJavapSignatures(boolean shouldUseJavapSignatures) {
        boolean originalValue = doUseJavapSignatures();

        if (originalValue != shouldUseJavapSignatures) {
            AgentApiGenerator.clearItems();
        }

        configMap.put(USE_JAVAP_SIGNATURES, shouldUseJavapSignatures);
    }

    public boolean isSavedFsVm(VmInfo vmInfo) {
        try {
            return getOrCreateSavedFsVms().contains(vmInfo.base64Serialize());
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.ALL, "Unable to determine if '" + vmInfo + "' is saved. Cause:");
            Logger.getLogger().log(e);
            return false;
        }
    }

    public void removeSavedFsVm(VmInfo vmInfo) throws IOException {
        getOrCreateSavedFsVms().remove(vmInfo.base64Serialize());
    }

    @SuppressWarnings("unchecked") // gson.fromJson returns a Map<String, Object> when called with configMap.getClass()
    private void loadConfigFile() throws IOException {
        configMap = new HashMap<>();
        File confFile = new File(CONFIG_PATH);
        File legacyConfFile = new File(LEGACY_CONFIG_PATH);
        if (confFile.exists()) {
            try (FileReader reader = new FileReader(confFile, StandardCharsets.UTF_8)) {
                Map<String, Object> tempMap = gson.fromJson(reader, configMap.getClass());
                if (tempMap != null) {
                    configMap = tempMap;
                }
            }
        } else if (legacyConfFile.exists()) {
            Files.readAllLines(Paths.get(LEGACY_CONFIG_PATH)).forEach(s -> {
                String[] kv = s.split("===");
                configMap.put(kv[0], kv[1]);
            });
            saveConfigFile();

            Files.delete(legacyConfFile.toPath());
        }
    }

    public void saveConfigFile() throws IOException {
        File confFile = new File(CONFIG_PATH);

        if (!confFile.getParentFile().exists()) {
            Files.createDirectories(confFile.getParentFile().toPath());
        }

        // creates file if it does not exist
        Files.write(Paths.get(CONFIG_PATH), Collections.singleton(gson.toJson(configMap)), StandardCharsets.UTF_8);
    }

}
