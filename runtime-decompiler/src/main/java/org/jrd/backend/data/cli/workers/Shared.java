package org.jrd.backend.data.cli.workers;

import org.jrd.backend.data.cli.Saving;
import org.jrd.frontend.frame.main.decompilerview.HexWithControls;
import org.jrd.frontend.utility.CommonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class Shared {

    private final Saving saving;
    private final boolean isHex;

    public Shared(boolean isHex, Saving saving) {
        this.isHex = isHex;
        this.saving = saving;
    }

    public boolean outOrSave(String name, String extension, String s) throws IOException {
        return outOrSave(name, extension, s.getBytes(StandardCharsets.UTF_8), false);
    }

    public boolean outOrSave(String name, String extension, byte[] body, boolean forceBytes) throws IOException {
        if (saving.shouldSave()) {
            return CommonUtils.saveByGui(saving.getAs(), saving.toInt(extension), extension, saving, name, body);
        } else {
            if (forceBytes) {
                if (isHex) {
                    System.out.println(HexWithControls.bytesToStrings(body).stream().collect(Collectors.joining("\n")));
                } else {
                    System.out.write(body);
                }
            } else {
                System.out.println(new String(body, StandardCharsets.UTF_8));
            }
            return true;
        }
    }
}
