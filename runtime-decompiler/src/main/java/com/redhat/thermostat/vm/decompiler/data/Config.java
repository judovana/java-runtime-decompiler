package com.redhat.thermostat.vm.decompiler.data;


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Singleton class for storing and retrieving configuration strings.
 */
public class Config {

    private static Config config;
    private HashMap<String, String> configMap;
    private String configFilePath;

    private Config() {
        String parentDir = new Directories().getConfigDirectory();
        /*File parentDirectoryFile = new File(parentDir);
        if (parentDirectoryFile.exists()) {
            if (parentDirectoryFile.isFile()) {
                System.err.println("Found a file where directory was expected");
            }
        } else {
            if (!parentDirectoryFile.mkdir()) {
                System.out.println(parentDirectoryFile.getPath());
                System.out.println("Directory couldn't be created");
            }
        }*/

        configFilePath = parentDir + "/config.cfg";

        try {
            loadConfigFile();
        } catch (IOException e) {
            e.printStackTrace();
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
            System.err.println("Agent must be a .jar file");
        }
    }

    public String getDecompilerName() {
        return configMap.get("DECOMPILER_NAME");
    }

    public void setDecompilerName(String decompilerPath) {
        configMap.put("DECOMPILER_NAME", decompilerPath);
    }

    private void loadConfigFile() throws IOException {
        configMap = new HashMap<>();
        File confFile = new File(configFilePath);
        if (confFile.exists()) {
            Files.readAllLines(Paths.get(configFilePath)).forEach(s -> {
                String[] kv = s.split("===");
                configMap.put(kv[0], kv[1]);
            });
        }
    }

    public void saveConfigFile() throws IOException {
        File confFile = new File(configFilePath);
        if (!confFile.exists()) {
            confFile.createNewFile();
        }
        List<String> lines = new ArrayList<>();
        for (String key : configMap.keySet()) {
            lines.add(key + "===" + configMap.get(key));
        }
        Files.write(Paths.get(configFilePath), lines, Charset.forName("UTF-8"));
    }

}
