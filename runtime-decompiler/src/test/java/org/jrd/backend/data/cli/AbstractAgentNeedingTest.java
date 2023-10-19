package org.jrd.backend.data.cli;

import org.jrd.backend.data.Config;
import org.jrd.backend.data.Directories;
import org.jrd.backend.data.Model;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractAgentNeedingTest {

    protected Model model;
    protected AbstractSourceTestClass dummy;
    protected final JunitStderrOutThief streams = new JunitStderrOutThief();

    abstract AbstractSourceTestClass dummyProvider() throws AbstractSourceTestClass.SourceTestClassWrapperException;

    @BeforeAll
    static void startup() throws IOException {
        setupAgentLocations();
    }

    static void setupAgentLocations() throws IOException {
        String maybeFreshAgent = findFreshAgent();
        if (maybeFreshAgent != null) {
            System.setProperty(Config.AGENT_PATH_OVERWRITE_PROPERTY, maybeFreshAgent);
        }

        String agentPath = Config.getConfig().getAgentExpandedPath();

        Assumptions.assumeTrue(!agentPath.isEmpty(), "Agent path is not set up, aborting CliTest.");
        Assumptions.assumeTrue(new File(agentPath).exists(), "Agent path is set up to nonexistent file, aborting CliTest.");
    }

    @Timeout(5)
    @BeforeEach
    void setup() throws InterruptedException {
        try {
            dummy = dummyProvider();
        } catch (AbstractSourceTestClass.SourceTestClassWrapperException e) {
            Assertions.fail(e);
        }
        Assertions.assertTrue(dummy.isAlive());

        model = Model.getModel();
        // VmManager
        // instantiation
        while (model.getVmManager().findVmFromPidNoException(dummy.getPid()) == null) {
            Thread.sleep(100);
            model.getVmManager().updateLocalVMs();
        }

        streams.captureStreams(true);
    }

    @AfterEach
    void cleanup() {
        streams.captureStreams(false);

        Assertions.assertTrue(dummy.isAlive());
        Assertions.assertTrue(dummy.isAlive());
        dummy.terminate();
    }

    private static boolean isDifferenceTolerable(double samenessPercentage, int actualChanges, int totalSize) {
        assert samenessPercentage >= 0 && samenessPercentage <= 1.0;
        return LevenshteinDistance.isDifferenceTolerableImpl(samenessPercentage, actualChanges, totalSize, false);
    }

    static void assertEqualsWithTolerance(String s1, String s2, double samenessPercentage) {
        Assertions.assertTrue(
                isDifferenceTolerable(samenessPercentage, LevenshteinDistance.calculate(s1, s2), Math.max(s1.length(), s2.length()))
        );
    }

    static void assertEqualsWithTolerance(List<String> l1, List<String> l2, double samenessPercentage) {
        // symmetric difference
        Set<String> intersection = new HashSet<>(l1);
        intersection.retainAll(l2);

        Set<String> difference = new HashSet<>();
        difference.addAll(l1);
        difference.addAll(l2);
        difference.removeAll(intersection);

        Assertions.assertTrue(isDifferenceTolerable(samenessPercentage, difference.size(), Math.max(l1.size(), l2.size())));
    }

    public static final class LevenshteinDistance {
        /**
         * Calculates the Levenshtein distance between two strings.<br/>
         * Uses a 2D array to represent individual changes, therefore the time complexity is quadratic
         * (in reference to the strings' length).
         *
         * @param str1 the first string
         * @param str2 the second string
         * @return an integer representing the amount of atomic changes between {@code str1} and {@code str2}
         */
        public static int calculate(String str1, String str2) {
            int[][] matrix = new int[str1.length() + 1][str2.length() + 1];

            for (int i = 0; i <= str1.length(); i++) {
                for (int j = 0; j <= str2.length(); j++) {
                    if (i == 0) { // distance between "" and str2 == how long str2 is
                        matrix[i][j] = j;
                    } else if (j == 0) { // distance between str1 and "" == how long str1 is
                        matrix[i][j] = i;
                    } else {
                        int substitution = matrix[i - 1][j - 1] + substitutionCost(str1.charAt(i - 1), str2.charAt(j - 1));
                        int insertion = matrix[i][j - 1] + 1;
                        int deletion = matrix[i - 1][j] + 1;

                        matrix[i][j] = min3(substitution, insertion, deletion);
                    }
                }
            }

            return matrix[str1.length()][str2.length()]; // result is in the bottom-right corner
        }

        private static int substitutionCost(char a, char b) {
            return (a == b) ? 0 : 1;
        }

        private static int min3(int a, int b, int c) {
            return Math.min(a, Math.min(b, c));
        }

        public static boolean isDifferenceTolerable(String s1, String s2, double samenessPercentage, boolean verbose) {
            return isDifferenceTolerableImpl(
                    samenessPercentage, LevenshteinDistance.calculate(s1, s2), Math.max(s1.length(), s2.length()), verbose
            );
        }

        public static boolean isDifferenceTolerableImpl(double samenessPercentage, int actualChanges, int totalSize, boolean verbose) {
            double changesAllowed = (1.0 - samenessPercentage) * totalSize;
            if (verbose) {
                System.err.println(100 - ((actualChanges * 100) / totalSize) + "% <? " + samenessPercentage * 100 + "%");
            }
            return actualChanges <= changesAllowed;
        }
    }

    public static class JunitStderrOutThief {
        private final ByteArrayOutputStream out;
        private final ByteArrayOutputStream err;

        private final PrintStream originalOut;
        private final PrintStream originalErr;

        JunitStderrOutThief() {
            out = new ByteArrayOutputStream();
            err = new ByteArrayOutputStream();
            originalOut = System.out;
            originalErr = System.err;
        }

        public void captureStreams(boolean capture) {
            if (capture) {
                out.reset();
                err.reset();
            }

            System.setOut(capture ? new PrintStream(out, true, StandardCharsets.UTF_8) : originalOut);
            System.setErr(capture ? new PrintStream(err, true, StandardCharsets.UTF_8) : originalErr);
        }

        private String get(ByteArrayOutputStream which) {
            String string = which.toString(StandardCharsets.UTF_8);
            which.reset();

            return string;
        }

        public String getOut() {
            return get(out);
        }

        public byte[] getOutBytes() {
            byte[] bytes = out.toByteArray();
            out.reset();

            return bytes;
        }

        public String getErr() {
            return get(err);
        }
    }

    public static String readBinaryAsString(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return readBinaryAsString(fis, "UTF-8", CodingErrorAction.REPLACE);
        }
    }

    public static String readBinaryAsString(FileInputStream input, String charBase, CodingErrorAction action) throws IOException {
        CharsetDecoder decoder = Charset.forName(charBase).newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        InputStreamReader reader = new InputStreamReader(input, decoder);
        StringBuilder sb = new StringBuilder();
        while (true) {
            int i = reader.read();
            if (i < 0) {
                break;
            }
            sb.append((char) i);
        }
        reader.close();
        return sb.toString();
    }

    private static String findFreshAgent() throws IOException {
        File agentDir = Directories.getFreshlyBuiltAgent();
        if (!agentDir.exists() || !agentDir.isDirectory()) {
            return null;
        }
        return agentDir.getCanonicalPath();
    }
}
