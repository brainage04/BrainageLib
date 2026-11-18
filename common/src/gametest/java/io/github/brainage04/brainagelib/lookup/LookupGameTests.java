package io.github.brainage04.brainagelib.lookup;

import io.github.brainage04.brainagelib.BrainageLib;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

public final class LookupGameTests {
    private LookupGameTests() {}

    /**
     * A server keeps one lookup, so every mod on it shares one share of Mojang's limit, and a
     * test's stub is what the server's callers get until it is reset.
     */
    public static void aServerKeepsOneLookupAndATestCanReplaceIt(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        NameLookup first = NameLookup.of(server, "BrainageLib test lookup", BrainageLib.LOGGER);
        helper.assertTrue(
                first == NameLookup.of(server, "another name", BrainageLib.LOGGER),
                "A server must keep one lookup, so its callers share one share of the limit");
        NameLookup stub = stub(server, name -> notFound());
        NameLookup.install(server, stub);
        try {
            helper.assertTrue(
                    stub == NameLookup.of(server, "BrainageLib test lookup", BrainageLib.LOGGER),
                    "An installed stub must be what the server's callers get");
        } finally {
            NameLookup.reset(server);
        }
        helper.assertTrue(
                stub != NameLookup.of(server, "BrainageLib test lookup", BrainageLib.LOGGER),
                "A reset must drop the stub");
        helper.succeed();
    }

    /**
     * The answer is handed to the server thread, not to the thread that asked, so a command can
     * act on it where the name cache and the saved data may be written.
     */
    public static void anAnswerIsDeliveredOnTheServerThread(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        NameAndId profile = new NameAndId(UUID.randomUUID(), "LookupGhost");
        ExecutorService worker =
                Executors.newSingleThreadExecutor(
                        runnable -> {
                            Thread thread = new Thread(runnable, "LookupGameTest worker");
                            thread.setDaemon(true);
                            return thread;
                        });
        AtomicReference<Thread> workerThread = new AtomicReference<>();
        AtomicReference<NameLookup.Answer> answer = new AtomicReference<>();
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        NameLookup.install(
                server,
                new NameLookup(
                        name -> {
                            workerThread.set(Thread.currentThread());
                            return found(profile);
                        },
                        worker,
                        server::execute,
                        server::isSameThread,
                        BrainageLib.LOGGER));
        try {
            NameLookup.of(server, "BrainageLib test lookup", BrainageLib.LOGGER)
                    .lookup(
                            "LookupGhost",
                            delivered -> {
                                callbackThread.set(Thread.currentThread());
                                answer.set(delivered);
                            });
            helper.runAfterDelay(
                    2,
                    () -> {
                        helper.assertTrue(answer.get() != null, "The answer must arrive");
                        helper.assertTrue(
                                workerThread.get() != null
                                        && workerThread.get() != Thread.currentThread(),
                                "The lookup must run off the server thread");
                        helper.assertTrue(
                                callbackThread.get() == Thread.currentThread(),
                                "The answer must be delivered on the server thread, not on "
                                        + workerThread.get().getName());
                        helper.assertTrue(
                                answer.get().status() == NameLookup.Status.FOUND,
                                "The stub's answer must be the one delivered, got "
                                        + answer.get().status());
                        helper.assertTrue(
                                profile.equals(answer.get().profile()),
                                "The answer must name the account the stub found");
                        worker.shutdownNow();
                        helper.succeed();
                    });
        } finally {
            NameLookup.reset(server);
        }
    }

    /** A server keeps one allowance per name, and clearing it frees every name at once. */
    public static void aServersAllowancesAreSharedAndClearedTogether(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CommandRateLimit share = CommandRateLimit.of(server);
        CommandRateLimit listing = CommandRateLimit.of(server, "listing");
        UUID player = UUID.randomUUID();
        try {
            helper.assertTrue(
                    share == CommandRateLimit.of(server),
                    "A server must keep one allowance for its main command");
            helper.assertTrue(
                    listing == CommandRateLimit.of(server, "listing"),
                    "A named allowance must be the same one on every call");
            helper.assertTrue(
                    share != listing, "A named allowance must be charged apart from the main one");
            helper.assertTrue(share.refuse(player) == 0, "The first use must be allowed");
            helper.assertTrue(
                    share.refuse(player) > 0, "A second use in the same second must be refused");
            helper.assertTrue(
                    listing.refuse(player) == 0,
                    "The main command's use must not spend the named allowance");
            CommandRateLimit.clearAll(server);
            helper.assertTrue(
                    share.refuse(player) == 0, "Clearing must free the main allowance");
            helper.assertTrue(
                    listing.refuse(player) == 0, "Clearing must free the named allowance");
        } finally {
            CommandRateLimit.clearAll(server);
        }
        helper.succeed();
    }

    private static NameLookup stub(MinecraftServer server, NameLookup.Repository repository) {
        return new NameLookup(
                repository,
                Runnable::run,
                server::execute,
                server::isSameThread,
                BrainageLib.LOGGER);
    }

    private static NameLookup.Answer found(NameAndId profile) {
        return new NameLookup.Answer(NameLookup.Status.FOUND, profile);
    }

    private static NameLookup.Answer notFound() {
        return new NameLookup.Answer(NameLookup.Status.NOT_FOUND, null);
    }
}
