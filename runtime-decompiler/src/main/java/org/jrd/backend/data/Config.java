package org.jrd.backend.data;


import org.jrd.backend.core.OutputController;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Collections;
import com.google.gson.Gson;
import org.jrd.backend.decompiling.ExpandableUrl;

import static org.jrd.backend.decompiling.ExpandableUrl.isOsWindows;


/**
 * Singleton class for storing and retrieving configuration strings.
 */
public class Config {

    private static Config config;
    private final Gson gson;
    private HashMap<String, String> configMap;
    private String configFilePath;
    private String legacyConfigFilePath;


    private Config() {
        String parentDir = Directories.getConfigDirectory();
        configFilePath = parentDir + File.separator + "config.json";
        legacyConfigFilePath = parentDir + File.separator + "config.cfg";
        gson = new Gson();

        try {
            loadConfigFile();
        } catch (IOException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
        }
    }

    public static synchronized Config getConfig() {
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    public String getAgentRawPath() {
        return ExpandableUrl.createFromPath(configMap.get("AGENT_PATH")).getRawPath();
    }

    public String getAgentExpandedPath() {
        String expandedPath = ExpandableUrl.createFromPath(configMap.get("AGENT_PATH")).getExpandedPath();
        if(isOsWindows() && expandedPath.length() > 0 && expandedPath.charAt(0) == '/') { // Agent attaching fails on Windows when path starts with a slash
            expandedPath = expandedPath.substring(1);
        }
        return expandedPath;
    }

    public void setAgentPath(String agentPath) {
        if (agentPath.endsWith(".jar")) {
            configMap.put("AGENT_PATH", ExpandableUrl.createFromPath(agentPath).getRawPath());
        } else {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Agent must be a .jar file"));
        }
    }

    private void loadConfigFile() throws IOException {
        configMap = new HashMap<>();
        configMap.put("AGENT_PATH", "");
        File confFile = new File(configFilePath);
        File legacyConfFile = new File(legacyConfigFilePath);
        if (confFile.exists()) {
            configMap = gson.fromJson(new FileReader(confFile), configMap.getClass());
        }else if (legacyConfFile.exists()){
            Files.readAllLines(Paths.get(legacyConfigFilePath)).forEach(s -> {
                String[] kv = s.split("===");
                configMap.put(kv[0], kv[1]);
            });
            saveConfigFile();
            legacyConfFile.delete();
        }
    }

    public void saveConfigFile() throws IOException {
        File confFile = new File(configFilePath);
        if (!confFile.getParentFile().exists()){
            confFile.getParentFile().mkdirs();
        }
        if (!confFile.exists()) {
            confFile.createNewFile();
        }
        Files.write(Paths.get(configFilePath), Collections.singleton(gson.toJson(configMap)), Charset.forName("UTF-8"));
    }

}
