package io.github.brainage04.brainagelib.confirmation;

import static io.github.brainage04.brainagelib.confirmation.ConfirmationWindow.Outcome.CONFIRMED;
import static io.github.brainage04.brainagelib.confirmation.ConfirmationWindow.Outcome.REQUESTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfirmationWindowTest {
    @Test
    void inclusiveWindowConfirmsOnItsLastTick() {
        var window = new ConfirmationWindow<String>(600, ConfirmationWindow.Boundary.INCLUSIVE);
        assertEquals(REQUESTED, window.requestOrConfirm("alex", 100));
        assertEquals(CONFIRMED, window.requestOrConfirm("alex", 700));
        assertEquals(REQUESTED, window.requestOrConfirm("alex", 800), "A confirmation is consumed");
        assertEquals(
                REQUESTED, window.requestOrConfirm("alex", 1401), "One tick late requests again");
        assertEquals(CONFIRMED, window.requestOrConfirm("alex", 2001));
    }

    @Test
    void exclusiveWindowClosesOnItsLastTick() {
        var window = new ConfirmationWindow<String>(200, ConfirmationWindow.Boundary.EXCLUSIVE);
        assertEquals(REQUESTED, window.requestOrConfirm("alex", 0));
        assertEquals(CONFIRMED, window.requestOrConfirm("alex", 199));
        assertEquals(REQUESTED, window.requestOrConfirm("alex", 200));
        assertEquals(CONFIRMED, window.requestOrConfirm("alex", 399));
        assertEquals(
                REQUESTED,
                window.requestOrConfirm("alex", 599),
                "An exclusive window closes exactly when it is that old");
    }

    @Test
    void keysAreIndependentAndCancelClearAndExpireDropRequests() {
        var window = new ConfirmationWindow<String>(200, ConfirmationWindow.Boundary.EXCLUSIVE);
        window.requestOrConfirm("alex", 0);
        window.requestOrConfirm("sam", 50);
        assertEquals(REQUESTED, window.requestOrConfirm("kim", 60));
        List<String> expired = new ArrayList<>();
        window.expire(199, expired::add);
        assertEquals(List.of(), expired);
        window.expire(200, expired::add);
        assertEquals(List.of("alex"), expired);
        window.cancel("sam");
        assertEquals(REQUESTED, window.requestOrConfirm("sam", 201));
        window.clear();
        assertEquals(REQUESTED, window.requestOrConfirm("kim", 201));
    }

    @Test
    void rejectsAnEmptyWindow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ConfirmationWindow<String>(0, ConfirmationWindow.Boundary.INCLUSIVE));
    }
}
