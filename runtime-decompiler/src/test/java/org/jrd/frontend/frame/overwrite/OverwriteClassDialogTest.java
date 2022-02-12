package org.jrd.frontend.frame.overwrite;

import org.jrd.backend.data.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class OverwriteClassDialogTest {

    @Test
    void testPurgeSourceTarget() {
        List<String> params = new ArrayList<>();
        Config.getConfig().setBestSourceTarget(Optional.of(8));
        OverwriteClassDialog.purgeSourceTarget(params);
        Assertions.assertEquals(Arrays.asList("-source", "8", "-target", "8"), params);
        Config.getConfig().setBestSourceTarget(Optional.of(11));
        OverwriteClassDialog.purgeSourceTarget(params);
        Assertions.assertEquals(Arrays.asList("-source", "11", "-target", "11"), params);
        Config.getConfig().setBestSourceTarget(Optional.of(8));
        params.add(0, "param1");
        params.add(1, "param2");
        OverwriteClassDialog.purgeSourceTarget(params);
        Assertions.assertEquals(Arrays.asList("-source", "8", "-target", "8", "param1", "param2"), params);
        Config.getConfig().setBestSourceTarget(Optional.of(11));
        OverwriteClassDialog.purgeSourceTarget(params);
        Assertions.assertEquals(Arrays.asList("-source", "11", "-target", "11", "param1", "param2"), params);
        Config.getConfig().setBestSourceTarget(Optional.of(8));
        params.add(0, "param3");
        params.add(1, "param4");
        OverwriteClassDialog.purgeSourceTarget(params);
        Assertions.assertEquals(Arrays.asList("-source", "8", "-target", "8", "param3", "param4", "param1", "param2"), params);
        params = new ArrayList<>();
        Config.getConfig().setBestSourceTarget(Optional.of(11));
        params.add(0, "param1");
        params.add(1, "param2");
        OverwriteClassDialog.purgeSourceTarget(params);
        Assertions.assertEquals(Arrays.asList("-source", "11", "-target", "11", "param1", "param2"), params);
    }

    @Test
    void testPurgeSourceTargetEmpty() {
        List<String> params = new ArrayList<>();
        Config.getConfig().setBestSourceTarget(Optional.empty());
        params.add(0, "param1");
        params.add(1, "param2");
        Exception exception = Assertions.assertThrows(NoSuchElementException.class, () -> OverwriteClassDialog.purgeSourceTarget(params));
    }
}
