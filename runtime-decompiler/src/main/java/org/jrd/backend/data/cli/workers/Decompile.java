package org.jrd.backend.data.cli.workers;

import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.Help;
import org.jrd.backend.data.cli.Lib;
import org.jrd.backend.data.cli.utils.PluginWithOptions;
import org.jrd.backend.data.cli.utils.Saving;
import org.jrd.backend.decompiling.PluginManager;

import java.io.File;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Decompile {

    private final List<String> filteredArgs;
    private final Saving saving;
    private final VmManager vmManager;
    private final PluginManager pluginManager;
    private final boolean isHex;

    public Decompile(boolean isHex, List<String> filteredArgs, Saving saving, VmManager vmManager, PluginManager pluginManager) {
        this.filteredArgs = filteredArgs;
        this.saving = saving;
        this.vmManager = vmManager;
        this.pluginManager = pluginManager;
        this.isHex = isHex;
    }

    public VmInfo decompile() throws Exception {
        if (filteredArgs.size() < 4) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.DECOMPILE_FORMAT + "'.");
        }

        VmInfo vmInfo = CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
        String plugin = filteredArgs.get(2);
        int failCount = 0;
        int classCount = 0;

        for (int i = 3; i < filteredArgs.size(); i++) {
            String clazzRegex = filteredArgs.get(i);
            List<String> classes =
                    Lib.obtainFilteredClasses(vmInfo, vmManager, Arrays.asList(Pattern.compile(clazzRegex)), false, Optional.empty())
                            .stream().map(a -> a.getName()).collect(Collectors.toList());

            for (String clazz : classes) {
                classCount++;
                VmDecompilerStatus result = Lib.obtainClass(vmInfo, clazz, vmManager);
                byte[] bytes = Base64.getDecoder().decode(result.getLoadedClassBytes());

                if (new File(plugin).exists() && plugin.toLowerCase().endsWith(".json")) {
                    throw new RuntimeException("Plugin loading directly from file is not implemented.");
                }

                PluginWithOptions pwo = Lib.getDecompilerFromString(plugin, pluginManager);

                if (pwo.getDecompiler() != null) {
                    String decompilationResult =
                            pluginManager.decompile(pwo.getDecompiler(), clazz, bytes, pwo.getOptions(), vmInfo, vmManager);

                    if (!new Shared(isHex, saving).outOrSave(clazz, ".java", decompilationResult)) {
                        failCount++;
                    }
                }
            }
        }
        CliUtils.returnNonzero(failCount, classCount);
        return vmInfo;
    }
}
