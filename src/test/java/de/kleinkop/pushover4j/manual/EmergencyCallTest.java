package de.kleinkop.pushover4j.manual;

import de.kleinkop.pushover4j.Message;
import de.kleinkop.pushover4j.Priority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmergencyCallTest extends ManualPushoverTest {

    private void emergencyTest() {
        var response = pushover().sendMessage(
            Message.of("This is just a test. Don't care.")
                .withTitle("Test only")
                .withPriority(Priority.EMERGENCY)
                .withDevice(device)
                .withTag("aTag")
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

        // Fetch state
        var emergencyState = pushover().getEmergencyState(receipt);
        assertEquals(1, emergencyState.status());
        assertFalse(emergencyState.expired());

        waiting(6L);

        // cancel emergency
        var cancel = pushover().cancelEmergencyMessage(receipt);
        assertEquals(1, cancel.status());

        waiting(7L);

        var newState = pushover().getEmergencyState(receipt);
        assertTrue(newState.expired());
    }

    public static void main(String[] args) {
        new EmergencyCallTest().emergencyTest();
    }
}
