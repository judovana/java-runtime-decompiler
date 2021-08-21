package org.jrd.backend.data;


import org.jrd.backend.core.OutputController;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Collections;
import com.google.gson.Gson;
import org.jrd.backend.decompiling.ExpandableUrl;

import static org.jrd.backend.data.Directories.isOsWindows;


/**
 * Singleton class for storing and retrieving configuration strings.
 */
public class Config {

    private final Gson gson;
    private HashMap<String, String> configMap;

    private static final String CONFIG_FILE_PATH = Directories.getConfigDirectory() + File.separator + "config.json";
    private static final String LEGACY_CONFIG_FILE_PATH = Directories.getConfigDirectory() + File.separator + "config.cfg";

    private static final String AGENT_PATH_KEY = "AGENT_PATH";

    private static class ConfigHolder {
        private static final Config INSTANCE = new Config();
    }

    private Config() {
        gson = new Gson();

        try {
            loadConfigFile();
        } catch (IOException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
        }
    }

    public static Config getConfig() {
        return ConfigHolder.INSTANCE;
    }

    public String getAgentRawPath() {
        return ExpandableUrl.createFromPath(configMap.get(AGENT_PATH_KEY)).getRawPath();
    }

    public String getAgentExpandedPath() {
        String expandedPath = ExpandableUrl.createFromPath(configMap.get(AGENT_PATH_KEY)).getExpandedPath();
        if(isOsWindows() && expandedPath.length() > 0 && expandedPath.charAt(0) == '/') { // Agent attaching fails on Windows when path starts with a slash
            expandedPath = expandedPath.substring(1);
        }
        return expandedPath;
    }

    public void setAgentPath(String agentPath) {
        if (agentPath.endsWith(".jar")) {
            configMap.put(AGENT_PATH_KEY, ExpandableUrl.createFromPath(agentPath).getRawPath());
        } else {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Agent must be a .jar file"));
        }
    }

    private void loadConfigFile() throws IOException {
        configMap = new HashMap<>();
        configMap.put(AGENT_PATH_KEY, "");
        File confFile = new File(CONFIG_FILE_PATH);
        File legacyConfFile = new File(LEGACY_CONFIG_FILE_PATH);
        if (confFile.exists()) {
            try (FileReader reader = new FileReader(confFile, StandardCharsets.UTF_8)){
                configMap = gson.fromJson(reader, configMap.getClass());
            }
        }else if (legacyConfFile.exists()){
            Files.readAllLines(Paths.get(LEGACY_CONFIG_FILE_PATH)).forEach(s -> {
                String[] kv = s.split("===");
                configMap.put(kv[0], kv[1]);
            });
            saveConfigFile();
            legacyConfFile.delete();
        }
    }

    public void saveConfigFile() throws IOException {
        File confFile = new File(CONFIG_FILE_PATH);
        if (!confFile.getParentFile().exists()){
            confFile.getParentFile().mkdirs();
        }
        if (!confFile.exists()) {
            confFile.createNewFile();
        }

        Files.write(Paths.get(CONFIG_FILE_PATH), Collections.singleton(gson.toJson(configMap)), StandardCharsets.UTF_8);
    }

}
