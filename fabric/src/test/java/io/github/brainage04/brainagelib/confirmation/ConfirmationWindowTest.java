package io.github.brainage04.brainagelib.confirmation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.brainage04.brainagelib.confirmation.ConfirmationWindow.Outcome.CONFIRMED;
import static io.github.brainage04.brainagelib.confirmation.ConfirmationWindow.Outcome.REQUESTED;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmationWindowTest {
    @Test void inclusiveWindowConfirmsOnItsLastTick() {
        var window = new ConfirmationWindow<String>(600, ConfirmationWindow.Boundary.INCLUSIVE);
        assertEquals(REQUESTED, window.requestOrConfirm("alex", 100));
        assertEquals(CONFIRMED, window.requestOrConfirm("alex", 700));
        assertEquals(REQUESTED, window.requestOrConfirm("alex", 800), "A confirmation is consumed");
        assertEquals(REQUESTED, window.requestOrConfirm("alex", 1401), "One tick late requests again");
        assertEquals(CONFIRMED, window.requestOrConfirm("alex", 2001));
    }

    @Test void exclusiveWindowClosesOnItsLastTick() {
        var window = new ConfirmationWindow<String>(200, ConfirmationWindow.Boundary.EXCLUSIVE);
        assertEquals(REQUESTED, window.requestOrConfirm("alex", 0));
        assertTrue(window.isPending("alex", 199));
        assertFalse(window.isPending("alex", 200));
        assertEquals(REQUESTED, window.requestOrConfirm("alex", 200));
        assertEquals(CONFIRMED, window.requestOrConfirm("alex", 399));
    }

    @Test void keysAreIndependentAndCancelClearAndExpireDropRequests() {
        var window = new ConfirmationWindow<String>(200, ConfirmationWindow.Boundary.EXCLUSIVE);
        window.requestOrConfirm("alex", 0);
        window.requestOrConfirm("sam", 50);
        assertEquals(REQUESTED, window.requestOrConfirm("kim", 60));
        List<String> expired = new ArrayList<>();
        window.expire(199, expired::add);
        assertEquals(List.of(), expired);
        window.expire(200, expired::add);
        assertEquals(List.of("alex"), expired);
        assertFalse(window.isPending("alex", 200));
        window.cancel("sam");
        assertEquals(REQUESTED, window.requestOrConfirm("sam", 201));
        window.clear();
        assertEquals(REQUESTED, window.requestOrConfirm("kim", 201));
    }

    @Test void rejectsAnEmptyWindow() {
        assertThrows(IllegalArgumentException.class, () -> new ConfirmationWindow<String>(0, ConfirmationWindow.Boundary.INCLUSIVE));
    }
}
