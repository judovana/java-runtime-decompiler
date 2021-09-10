package org.jrd.backend.data;

import com.google.gson.Gson;
import org.jrd.backend.core.Logger;
import org.jrd.backend.decompiling.ExpandableUrl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class for storing and retrieving configuration strings.
 */
public final class Config {

    private final Gson gson;
    private Map<String, Object> configMap;

    private static final String CONFIG_PATH = Directories.getConfigDirectory() + File.separator + "config.json";
    private static final String LEGACY_CONFIG_PATH = Directories.getConfigDirectory() + File.separator + "config.cfg";

    private static final String AGENT_PATH_KEY = "AGENT_PATH";
    private static final String SAVED_FS_VMS_KEY = "FS_VMS";
    private static final String USE_HOST_SYSTEM_CLASSES_KEY = "USE_HOST_SYSTEM_CLASSES";

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

    public String getAgentRawPath() {
        return ExpandableUrl.createFromPath((String) configMap.get(AGENT_PATH_KEY)).getRawPath();
    }

    public String getAgentExpandedPath() {
        String expandedPath = ExpandableUrl.createFromPath((String) configMap.get(AGENT_PATH_KEY)).getExpandedPath();

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

    public boolean doUseHostSystemClasses() {
        return (boolean) configMap.getOrDefault(USE_HOST_SYSTEM_CLASSES_KEY, true);
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

    private void loadConfigFile() throws IOException {
        configMap = new HashMap<>();
        configMap.put(AGENT_PATH_KEY, "");
        File confFile = new File(CONFIG_PATH);
        File legacyConfFile = new File(LEGACY_CONFIG_PATH);
        if (confFile.exists()) {
            try (FileReader reader = new FileReader(confFile, StandardCharsets.UTF_8)) {
                configMap = gson.fromJson(reader, configMap.getClass());
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
