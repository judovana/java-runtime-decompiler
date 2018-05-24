package com.redhat.thermostat.vm.decompiler.data;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Config {

    private static Config config = new Config();

    public static Config getConfig(){
        return config;
    }

    public HashMap<String, String> configMap;
    private String configFilePath;

    public String getAgentPath() {
        return configMap.get("AGENT_PATH");
    }

    public void setAgentPath(String agentPath) {
        if (agentPath.endsWith(".jar")){
            configMap.put("AGENT_PATH", agentPath);
        } else {
            System.err.println("Agent must be a .jar file");
        }
    }

    public String getDecompilerPath() {
        return configMap.get("DECOMPILER_PATH");
    }

    public void setDecompilerPath(String decompilerPath) {
        if (decompilerPath.endsWith(".jar")){
            configMap.put("DECOMPILER_PATH", decompilerPath);
        } else {
            System.err.println("Decomiler must be a .jar file");
        }
    }

    Config(){
        String parentDir = null;
        try {
            parentDir = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        configFilePath = parentDir + "/config.cfg";
        try {
            loadConfigFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfigFile() throws IOException {
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
        if (!confFile.exists()){
            confFile.createNewFile();
        }
        List<String> lines = new ArrayList<>();
        for (String key: configMap.keySet()){
            lines.add(key + "===" + configMap.get(key));
        }
        Files.write(Paths.get(configFilePath), lines, Charset.forName("UTF-8"));
    }

}
