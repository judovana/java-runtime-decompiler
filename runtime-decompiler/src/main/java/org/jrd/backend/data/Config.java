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


/**
 * Singleton class for storing and retrieving configuration strings.
 */
public class Config {

    private static Config config;
    private static Gson gson;
    private HashMap<String, String> configMap;
    private String configFilePath;


    private Config() {
        String parentDir = Directories.getConfigDirectory();
        configFilePath = parentDir + "/config.json";
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

    public String getAgentPath() {
        return configMap.get("AGENT_PATH");
    }

    public void setAgentPath(String agentPath) {
        if (agentPath.endsWith(".jar")) {
            configMap.put("AGENT_PATH", agentPath);
        } else {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Agent must be a .jar file"));
        }
    }

    private void loadConfigFile() throws IOException {
        configMap = new HashMap<>();
        File confFile = new File(configFilePath);
        if (confFile.exists()) {
            configMap = gson.fromJson(new FileReader(confFile), configMap.getClass());
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
