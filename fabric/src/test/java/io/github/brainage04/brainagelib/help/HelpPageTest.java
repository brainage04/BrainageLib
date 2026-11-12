package io.github.brainage04.brainagelib.help;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.brainage04.brainagelib.feedback.ModFeedback;
import org.junit.jupiter.api.Test;

class HelpPageTest {
    private final HelpPage page = HelpPage.create(ModFeedback.create("Example"), "example");

    @Test
    void blankLinesAreRejectedWhenTheyAreAdded() {
        assertThrows(IllegalArgumentException.class, () -> page.line(""));
        assertThrows(IllegalArgumentException.class, () -> page.line("   "));
        assertThrows(NullPointerException.class, () -> page.line(null));
        assertDoesNotThrow(() -> page.line("/example - show this page"));
    }
}
