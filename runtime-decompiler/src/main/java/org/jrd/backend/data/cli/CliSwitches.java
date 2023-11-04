package org.jrd.backend.data.cli;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class CliSwitches {

    public static final String VERBOSE = "-verbose";
    public static final String CONFIG = "-config";
    public static final String HEX = "-hex";
    public static final String SYSTEM_CLASS_LOADER = "SYSTEM";
    public static final String BOOT_CLASS_LOADER = "BOOT";
    public static final String SAVE_AS = "-saveas";
    public static final String SAVE_LIKE = "-savelike";
    public static final String CLASSLOADER_SET = "-classloader";
    public static final String LIST_CLASSLOADERS = "-listclassloaders";
    public static final String LIST_JVMS = "-listjvms";
    public static final String LIST_OVERRIDES = "-listoverrides";
    public static final String REMOVE_OVERRIDES = "-removeoverrides";
    public static final String LIST_PLUGINS = "-listplugins";
    public static final String LIST_AGENTS = "-listagents";
    public static final String LIST_CLASSES = "-listclasses";
    public static final String SEARCH = "-search";
    public static final String LIST_CLASSESDETAILS = "-listdetails";
    public static final String LIST_CLASSESBYTECODEVERSIONS = "-listbytecodeversions";
    public static final String LIST_CLASSESDETAILSBYTECODEVERSIONS = "-listdetailsversions";
    public static final String BASE64 = "-base64bytes";
    public static final String BYTES = "-bytes";
    public static final String DEPS = "-deps";
    public static final String DECOMPILE = "-decompile";
    public static final String COMPILE = "-compile";
    public static final String OVERWRITE = "-overwrite";
    public static final String ADD_CLASS = "-addclass";
    public static final String ADD_JAR = "-addjar";
    public static final String ADD_CLASSES = "-addclasses";
    public static final String PATCH = "-patch";
    public static final String INIT = "-init";
    public static final String AGENT = "-agent";
    public static final String ATTACH = "-attach";
    public static final String DETACH = "-detach";
    public static final String API = "-api";
    public static final String COMPLETION = "-completion";
    public static final String VERSION = "-version";
    public static final String VERSIONS = "-versions";
    public static final String HELP = "-help";
    public static final String H = "-h";
    public static final String REVERT = "-R";

    public static final String R = "-r";
    public static final String P = "-p";
    public static final String CP = "-cp";

    private CliSwitches() {
    }

    public static List<String> getSwitches() throws IllegalAccessException {
        Field[] fields = CliSwitches.class.getDeclaredFields();
        List<String> all = new ArrayList<>(fields.length);
        for (Field f : fields) {
            all.add(f.get(null).toString());
        }
        return all;
    }

    public static boolean isSwitch(String s) throws IllegalAccessException {
        String ss = CliUtils.cleanParameter(s);
        return getSwitches().contains(ss);
    }

    public static boolean noMatch(List<String> filteredArgs) {
        try {
            for (String arg : filteredArgs) {
                if (isSwitch(arg)) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
