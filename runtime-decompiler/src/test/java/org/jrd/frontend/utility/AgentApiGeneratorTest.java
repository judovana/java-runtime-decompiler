package org.jrd.frontend.utility;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentApiGeneratorTest {

    @Test
    void containsAllInOrderEmptyes() {
        Assertions.assertTrue(AgentApiGenerator.containsAllInOrder(null, "blah"));
        Assertions.assertTrue(AgentApiGenerator.containsAllInOrder("", "blah"));
        Assertions.assertTrue(AgentApiGenerator.containsAllInOrder("    ", "blah"));
        Assertions.assertFalse(AgentApiGenerator.containsAllInOrder("blah", null));
        Assertions.assertFalse(AgentApiGenerator.containsAllInOrder("blah", ""));
        Assertions.assertFalse(AgentApiGenerator.containsAllInOrder("blah", "   "));
    }

    @Test
    void containsAllInOrder() {
        Assertions.assertTrue(AgentApiGenerator.containsAllInOrder("abc", "xxxayyyyaaadddbbbuuuceee"));
        Assertions.assertTrue(AgentApiGenerator.containsAllInOrder("abbc", "xxxayyyyaaadddbbbuuuceee"));
        Assertions.assertFalse(AgentApiGenerator.containsAllInOrder("abbc", "xxxayyyyaaadddbuuuceee"));
        Assertions.assertTrue(AgentApiGenerator.containsAllInOrder("blah", "blah"));
        Assertions.assertTrue(AgentApiGenerator.containsAllInOrder("bh", "blah  "));
        Assertions.assertTrue(AgentApiGenerator.containsAllInOrder("  l  ", "blah"));
        Assertions.assertFalse(AgentApiGenerator.containsAllInOrder("hlab", "blah"));
        Assertions.assertFalse(AgentApiGenerator.containsAllInOrder("x", "blah"));
        Assertions.assertFalse(AgentApiGenerator.containsAllInOrder("bha", "blah"));
        Assertions.assertFalse(AgentApiGenerator.containsAllInOrder("ab", "blah"));
    }

    @Test
    void containsAllInOrderSplit() {
        Assertions.assertTrue(AgentApiGenerator.containsAllInOrder("ab", "xxxayy.yyb"));
        Assertions.assertFalse(AgentApiGenerator.containsAllInOrder("ab", "xxxayy..yyb"));
    }
}
