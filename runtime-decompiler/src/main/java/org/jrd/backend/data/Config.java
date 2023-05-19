package org.jrd.backend.data;

import com.google.gson.Gson;
import org.jrd.backend.communication.ErrorCandidate;
import org.jrd.backend.communication.FsAgent;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.Logger;
import org.jrd.backend.decompiling.ExpandableUrl;
import org.jrd.frontend.utility.AgentApiGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private static final String DETECT_AUTOCOMPLETION = "DETECT_AUTOCOMPLETION";
    private static final String ENFORCE_SOURCE_TARGET = "ENFORCE_SOURCE_TARGET";
    private static final String DEPNDENCE_NUMBERS = "DEPNDENCE_NUMBERS";
    private static final String ADDITIONAL_SOURCE_PATH = "ADDITIONAL_SOURCE_PATH";
    private static final String ADDITIONAL_CLASS_PATH = "ADDITIONAL_CLASS_PATH";
    //this is not persistent, is used for transfering detected value to compiler with other settings
    private Optional<Integer> sourceTargetValue;
    private FsAgent additionalClassPathAgent;
    private FsAgent additionalSourcePathAgent;

    public enum DepndenceNumbers {
        ENFORCE_ONE("This will pass only selected class to decompiler. Fastest, worst results, may have its weird usecase"),
        ALL_INNERS("Together with selected class, also all its inner classes are send to decompiler. Fast. Good enough results"),
        ALL(
                "Together with selected class, also all classes it depends on  are send to decompiler." +
                        " Slow, best results. Also it forces java.* internal classes from host, not local"
        );

        public final String description;

        DepndenceNumbers(String s) {
            description = s;
        }

        public static DepndenceNumbers fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(DepndenceNumbers.values()).filter(v -> v.toString().equals(s)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    public DepndenceNumbers getDepndenciesNumber() {
        return getConfig().doDepndenceNumbers();
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

    public void setOverwriteST(boolean overwriteST) {
        configMap.put(ENFORCE_SOURCE_TARGET, overwriteST);
    }

    public void setDepndenceNumbers(DepndenceNumbers dn) {
        configMap.put(DEPNDENCE_NUMBERS, dn);
    }

    public boolean doUseHostSystemClasses() {
        return (boolean) configMap.getOrDefault(USE_HOST_SYSTEM_CLASSES_KEY, true);
    }

    public boolean doUseHostJavaLangObject() {
        return (boolean) configMap.getOrDefault(USE_HOST_JAVA_LANG_OBJECT, true);
    }

    public boolean doOverwriteST() {
        return (boolean) configMap.getOrDefault(ENFORCE_SOURCE_TARGET, true);
    }

    public DepndenceNumbers doDepndenceNumbers() {
        return DepndenceNumbers.fromString((configMap.getOrDefault(DEPNDENCE_NUMBERS, DepndenceNumbers.ALL_INNERS.toString())).toString());
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

    public boolean doAutocompletion() {
        return (boolean) configMap.getOrDefault(DETECT_AUTOCOMPLETION, true);
    }

    public void setUseJavapSignatures(boolean shouldUseJavapSignatures) {
        boolean originalValue = doUseJavapSignatures();

        if (originalValue != shouldUseJavapSignatures) {
            AgentApiGenerator.clearItems();
        }

        configMap.put(USE_JAVAP_SIGNATURES, shouldUseJavapSignatures);
    }

    public void setAutocomplete(boolean shouldAutocomplete) {
        configMap.put(DETECT_AUTOCOMPLETION, shouldAutocomplete);
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
        File confFile = getConfFile();
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
        initAdditionalAgents();
    }

    private void initAdditionalAgents() {
        if (!getAdditionalCP().trim().isEmpty()) {
            try {
                this.additionalClassPathAgent = FsAgent.createAdditionalClassPathFsAgent(
                        Arrays.stream(getAdditionalCP().split(File.pathSeparator)).map(a -> new File(a)).collect(Collectors.toList())
                );
            } catch (Exception ex) {
                additionalClassPathAgent = null;
                Logger.getLogger().log(ex);
            }
        } else {
            this.additionalClassPathAgent = null;
        }
        if (!getAdditionalSP().trim().isEmpty()) {
            try {
                this.additionalSourcePathAgent = FsAgent.createAdditionalSourcePathFsAgent(
                        Arrays.stream(getAdditionalSP().split(File.pathSeparator)).map(a -> new File(a)).collect(Collectors.toList())
                );
            } catch (Exception ex) {
                additionalSourcePathAgent = null;
                Logger.getLogger().log(ex);
            }
        } else {
            this.additionalSourcePathAgent = null;
        }
    }

    public void saveConfigFile() throws IOException {
        File confFile = getConfFile();

        if (!confFile.getParentFile().exists()) {
            Files.createDirectories(confFile.getParentFile().toPath());
        }

        // creates file if it does not exist
        Files.write(Paths.get(CONFIG_PATH), Collections.singleton(gson.toJson(configMap)), StandardCharsets.UTF_8);
        initAdditionalAgents();
    }

    public File getConfFile() {
        return new File(CONFIG_PATH);
    }

    public Optional<Integer> getBestSourceTarget() {
        if (sourceTargetValue == null) {
            return Optional.empty();
        }
        return sourceTargetValue;
    }

    public void setBestSourceTarget(Optional<Integer> st) {
        //how to fill it from cli?
        //cli api to print bytecode levels?
        //yes, not much more
        this.sourceTargetValue = st;
    }

    public void setAdditionalCP(String paths) {
        configMap.put(ADDITIONAL_CLASS_PATH, paths);
    }

    public void setAdditionalSP(String paths) {
        configMap.put(ADDITIONAL_SOURCE_PATH, paths);
    }

    public String getAdditionalCP() {
        Object s = configMap.get(ADDITIONAL_CLASS_PATH);
        if (s == null) {
            s = "";
        }
        return s.toString();
    }

    public String getAdditionalSP() {
        Object s = configMap.get(ADDITIONAL_SOURCE_PATH);
        if (s == null) {
            s = "";
        }
        return s.toString();
    }

    public byte[] getAdditionalClassPathBytes(String fqn) {
        if (additionalClassPathAgent == null) {
            return new byte[0];
        } else {
            try {
                return getFileFromAdditionalPath(additionalClassPathAgent, fqn);
            } catch (Exception ex) {
                Logger.getLogger().log(ex);
                return new byte[0];
            }
        }
    }

    public String[] getAdditionalClassPathListing() {
        if (additionalClassPathAgent == null) {
            return new String[0];
        } else {
            try {
                return getListingFromAdditionalPath(additionalClassPathAgent);
            } catch (Exception ex) {
                Logger.getLogger().log(ex);
                return new String[0];
            }
        }
    }

    public String getAdditionalSourcePathString(String fqn) {
        if (additionalSourcePathAgent == null) {
            return "";
        } else {
            try {
                if (fqn.contains("$")) {
                    try {
                        //there are weird classes which have $ as valid character
                        byte[] bytes = getFileFromAdditionalPath(additionalSourcePathAgent, fqn);
                        return new String(bytes, Charset.defaultCharset());
                    } catch (Exception ex) {
                        byte[] bytes = getFileFromAdditionalPath(additionalSourcePathAgent, fqn.replaceAll("\\$.*", ""));
                        return "/*WARNING! showing wrapper class! Do not use for upload!*/\n" + "/*The class amy still be used as " + fqn +
                                " WARNING!*/\n" + new String(bytes, Charset.defaultCharset());
                    }
                } else {
                    byte[] bytes = getFileFromAdditionalPath(additionalSourcePathAgent, fqn);
                    return new String(bytes, Charset.defaultCharset());
                }
            } catch (Exception ex) {
                Logger.getLogger().log(ex);
                return ex.getMessage();
            }
        }
    }

    private byte[] getFileFromAdditionalPath(FsAgent fs, String fqn) {
        String base64 = fs.submitRequest(AgentRequestAction.RequestAction.BYTES + " " + fqn);
        ErrorCandidate errorCandidate = new ErrorCandidate(base64);
        if (errorCandidate.isError()) {
            throw new RuntimeException(errorCandidate.getErrorMessage());
        }
        byte[] bbytes = Base64.getDecoder().decode(base64);
        return bbytes;
    }

    private String[] getListingFromAdditionalPath(FsAgent fs) {
        String classes = fs.submitRequest(AgentRequestAction.RequestAction.CLASSES + "");
        ErrorCandidate errorCandidate = new ErrorCandidate(classes);
        if (errorCandidate.isError()) {
            throw new RuntimeException(errorCandidate.getErrorMessage());
        }
        String[] r = classes.split(";");
        return r;
    }
}
