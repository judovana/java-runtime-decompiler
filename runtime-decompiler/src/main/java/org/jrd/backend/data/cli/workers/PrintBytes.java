package org.jrd.backend.data.cli.workers;

import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.data.DependenciesReader;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.utils.CompileArguments;
import org.jrd.backend.data.cli.Help;
import org.jrd.backend.data.cli.Lib;
import org.jrd.backend.data.cli.utils.Saving;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.LoadingDialogProvider;
import org.jrd.frontend.frame.main.ModelProvider;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.jrd.backend.data.cli.CliSwitches.BYTES;
import static org.jrd.backend.data.cli.CliSwitches.CP;
import static org.jrd.backend.data.cli.CliSwitches.DEPS;

public class PrintBytes {

    private final List<String> filteredArgs;
    private final Saving saving;
    private final VmManager vmManager;
    private final PluginManager pluginManager;
    private final boolean isHex;

    public PrintBytes(boolean isHex, List<String> filteredArgs, Saving saving, VmManager vmManager, PluginManager pluginManager) {
        this.filteredArgs = filteredArgs;
        this.saving = saving;
        this.vmManager = vmManager;
        this.pluginManager = pluginManager;
        this.isHex = isHex;
    }

    public VmInfo printBytes(String operation) throws Exception {
        if (filteredArgs.size() < 3) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + (Help.BASE_SHARED_FORMAT) + "'.");
        }
        final CompileArguments args;
        if (operation.equals(DEPS)) {
            List nwArgs = new ArrayList<>(filteredArgs);
            nwArgs.set(0, CP); //faking a bit
            nwArgs.add(0, "dummy"); //faking a bit more
            args = new CompileArguments(nwArgs, pluginManager, vmManager, false);
        } else {
            args = null;
        }
        VmInfo vmInfo = CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
        int failCount = 0;
        int classCount = 0;

        for (int i = 2; i < filteredArgs.size(); i++) {
            String clazzRegex = filteredArgs.get(i);
            List<String> classes =
                    Lib.obtainFilteredClasses(vmInfo, vmManager, Arrays.asList(Pattern.compile(clazzRegex)), false, Optional.empty())
                            .stream().map(a -> a.getName()).collect(Collectors.toList());

            for (String clazz : classes) {
                classCount++;
                VmDecompilerStatus result = Lib.obtainClass(vmInfo, clazz, vmManager);
                byte[] bytes;
                if (operation.equals(BYTES)) {
                    bytes = Base64.getDecoder().decode(result.getLoadedClassBytes());
                } else if (operation.equals(DEPS)) {
                    Collection<String> deps = new DependenciesReader(new ModelProvider() {
                        @Override
                        public VmInfo getVmInfo() {
                            return vmInfo;
                        }

                        @Override
                        public VmManager getVmManager() {
                            return vmManager;
                        }

                        @Override
                        public RuntimeCompilerConnector.JrdClassesProvider getClassesProvider() {
                            return args.getClassesProvider();
                        }
                    }, new LoadingDialogProvider() {
                    }).resolve(clazz, result.getLoadedClassBytes());
                    bytes = deps.stream().collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8);
                } else {
                    bytes = result.getLoadedClassBytes().getBytes(StandardCharsets.UTF_8);
                }

                if (!new Shared(isHex, saving).outOrSave(clazz, ".class", bytes, operation.equals(BYTES))) {
                    failCount++;
                }
            }
        }
        CliUtils.returnNonzero(failCount, classCount);
        return vmInfo;
    }
}
