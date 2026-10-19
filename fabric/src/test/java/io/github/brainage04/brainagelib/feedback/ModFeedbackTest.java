package io.github.brainage04.brainagelib.feedback;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModFeedbackTest {
    @Test
    void decorationCopiesTheMessageAndPreservesItsInteractiveStyle() {
        var original = Component.literal("Open help").withStyle(style -> style.withBold(true).withInsertion("/example help"));
        var decorated = ModFeedback.create(" Example ").decorate(ChatFormatting.GREEN, original);
        assertEquals("[Example] Open help", decorated.getString());
        assertEquals(0xAAAAAA, decorated.getStyle().getColor().getValue());
        var body = decorated.getSiblings().getFirst();
        assertEquals(0x55FF55, body.getStyle().getColor().getValue());
        assertTrue(body.getStyle().isBold());
        assertEquals("/example help", body.getStyle().getInsertion());
        assertNull(original.getStyle().getColor());
        original.append(" changed");
        assertEquals("[Example] Open help", decorated.getString());
    }

    @Test
    void blankModNamesAreRejectedInsteadOfProducingAnEmptyPrefix() {
        assertThrows(IllegalArgumentException.class, () -> ModFeedback.create(" "));
    }
}
