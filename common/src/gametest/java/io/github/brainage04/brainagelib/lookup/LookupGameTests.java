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
        NameLookup first = NameLookup.of(server, "BrainageLib name lookup", BrainageLib.LOGGER);
        helper.assertTrue(
                first == NameLookup.of(server, "another name", BrainageLib.LOGGER),
                "A server must keep one lookup, so its callers share one share of the limit");
        NameLookup stub = stub(server, name -> notFound());
        NameLookup.install(server, stub);
        AtomicReference<NameLookup.Answer> answer = new AtomicReference<>();
        try {
            NameLookup installed =
                    NameLookup.of(server, "BrainageLib name lookup", BrainageLib.LOGGER);
            helper.assertTrue(
                    stub == installed, "An installed stub must be what the server's callers get");
            installed.lookup("LookupGhost", answer::set);
            helper.assertTrue(
                    answer.get() != null && answer.get().status() == NameLookup.Status.NOT_FOUND,
                    "A lookup through the server's lookup must be the stub's answer, got "
                            + answer.get());
        } finally {
            NameLookup.reset(server);
        }
        helper.assertTrue(
                stub != NameLookup.of(server, "BrainageLib name lookup", BrainageLib.LOGGER),
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
        // Its own lookup, not the server's: the tests of one batch tick interleaved, so a test that
        // installed a stub here would race the one that checks the installed stub.
        NameLookup lookup =
                new NameLookup(
                        name -> {
                            workerThread.set(Thread.currentThread());
                            return found(profile);
                        },
                        worker,
                        server::execute,
                        server::isSameThread,
                        BrainageLib.LOGGER);
        lookup.lookup(
                "LookupGhost",
                delivered -> {
                    callbackThread.set(Thread.currentThread());
                    answer.set(delivered);
                });
        helper.runAfterDelay(
                2,
                () -> {
                    // Shut down here, not when this method returns: shutdownNow drops a task that
                    // has not started, and the lookup is still queued at that point.
                    try {
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
                    } finally {
                        worker.shutdownNow();
                    }
                    helper.succeed();
                });
    }

    /** A server keeps one allowance per name, and a named one is charged apart from the main. */
    public static void aServersAllowancesAreSharedAndChargedApart(GameTestHelper helper) {
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
            share.clear();
            helper.assertTrue(
                    share.refuse(player) == 0, "Clearing must free the main allowance");
            listing.clear();
            helper.assertTrue(
                    listing.refuse(player) == 0, "Clearing must free the named allowance");
        } finally {
            // Only this test's two allowances: clearAll would wipe a consumer mod's as well.
            share.clear();
            listing.clear();
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
