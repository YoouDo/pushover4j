package de.kleinkop.pushover4j.manual;

import java.time.OffsetDateTime;

import de.kleinkop.pushover4j.Message;
import de.kleinkop.pushover4j.Priority;

public class SimplePushoverCall extends ManualPushoverTest {

    private void simpleTest() {
        pushover()
            .sendMessage(
                Message.of("Testing")
                    .withTitle("Title")
                    .withPriority(Priority.LOW)
                    .withDevice(device)
                    .withTimestamp(OffsetDateTime.now())
                    .withHtml(true)
                    .withMonospace(false)
                    .build()
            );

        pushover()
            .getSounds()
            .sounds()
            .forEach((s, s2) -> System.out.println(s + " -> " + s2));
    }

    public static void main(String[] args) {
        new SimplePushoverCall().simpleTest();
    }
}
