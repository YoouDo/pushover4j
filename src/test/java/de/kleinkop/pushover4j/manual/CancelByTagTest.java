package de.kleinkop.pushover4j.manual;

import de.kleinkop.pushover4j.Message;
import de.kleinkop.pushover4j.Priority;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CancelByTagTest extends ManualPushoverTest {

    private static final Logger log = LoggerFactory.getLogger(CancelByTagTest.class);

    void cancelTest() {
        final String tag = "aTag";

        var response = pushover().sendMessage(
            Message.of("This is just a test. Don't care.")
                .withTitle("Test only")
                .withPriority(Priority.EMERGENCY)
                .withDevice(device)
                .withTag(tag)
                .withTag("testing")
                .withTag("ignore")
                .withRetry(30)
                .withExpiration(120)
                .build()
        );

        assertEquals(1, response.status());

        var receipt = response.receipt();
        assertNotNull(receipt);

        waiting(6L);

        // fetch state
        var emergencyState = pushover().getEmergencyState(receipt);
        assertEquals(1, emergencyState.status());
        assertFalse(emergencyState.expired());

        waiting(6L);

        var cancel = pushover().cancelEmergencyMessageByTag(tag);
        log.info("Cancel result: {}", cancel);

        assertEquals(1, cancel.status());
        assertEquals(1, cancel.canceled());

        waiting(7L);
        assertTrue(pushover().getEmergencyState(receipt).expired());
    }

    public static void main(String[] args) {
        new CancelByTagTest().cancelTest();
    }
}
