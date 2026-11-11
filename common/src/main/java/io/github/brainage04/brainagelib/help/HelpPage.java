package io.github.brainage04.brainagelib.help;

import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.brainagelib.feedback.ModFeedback;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;

/**
 * A mod's {@code /<mod>} help page: one heading line and one line per command, printed through the
 * mod's {@link ModFeedback}, with the root and its {@code help} child registered together.
 */
public final class HelpPage {
    private static final String DEFAULT_HEADING = "Player commands:";

    private final ModFeedback feedback;
    private final String command;
    private final List<Line> lines = new ArrayList<>();
    private String heading = DEFAULT_HEADING;

    private HelpPage(ModFeedback feedback, String command) {
        this.feedback = Objects.requireNonNull(feedback, "feedback");
        this.command = requireCommand(command);
    }

    /** A page printed by {@code /command} and {@code /command help}. */
    public static HelpPage create(ModFeedback feedback, String command) {
        return new HelpPage(feedback, command);
    }

    /** Replaces the first line; defaults to "Player commands:". */
    public HelpPage heading(String heading) {
        String text = Objects.requireNonNull(heading, "heading");
        if (text.isBlank()) throw new IllegalArgumentException("heading must not be blank");
        this.heading = text;
        return this;
    }

    /** Adds one command line; the message takes {@link String#formatted} arguments. */
    public HelpPage line(String message, Object... arguments) {
        lines.add(new Line(message, arguments.clone()));
        return this;
    }

    /** Registers {@code /command} and {@code /command help} to print this page. */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        dispatcher.register(
                literal(command)
                        .executes(context -> show(context.getSource()))
                        .then(literal("help").executes(context -> show(context.getSource()))));
    }

    /** Prints the heading and the lines through the mod's feedback, and returns 1. */
    public int show(CommandSourceStack source) {
        feedback.neutral(source, heading);
        for (Line line : lines) feedback.neutral(source, line.message(), line.arguments());
        return 1;
    }

    private static String requireCommand(String command) {
        String text = Objects.requireNonNull(command, "command");
        if (text.isBlank()
                || text.startsWith("/")
                || text.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "command must be a command name without '/' or spaces: " + command);
        }
        return text;
    }

    private record Line(String message, Object[] arguments) {}
}
