package de.kleinkop.pushover4j.manual;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.kleinkop.pushover4j.Message;
import de.kleinkop.pushover4j.Priority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncryptedPushoverCall extends ManualPushoverCall {

    private static final Logger log = LoggerFactory.getLogger(EncryptedPushoverCall.class);

    private void simpleTest() {
        var sounds = pushover()
            .getSounds()
            .sounds()
            .keySet();

        assertThat(sounds, Matchers.hasItem(sound));

        var file = file("/testdata/image.png");

        assertTrue(file.exists());

        var key = IntStream.range(0, 64).mapToObj(i -> "0").collect(Collectors.joining());
        var pushoverResponse = pushover()
            .sendEncryptedMessage(
                Message.of("Testnachricht: äöüß \uD83E\uDD37 <br/>This message will destroy itself in 30 seconds!")
                    .withTitle("A title (äöüß)")
                    .withPriority(Priority.NORMAL)
                    .withDevice(device)
                    .withTimestamp(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                    .withHtml(true)
                    .withMonospace(false)
                    .withImage(file)
                    .withUrl("https://www.example.com")
                    .withUrlTitle("This is an example URL.")
                    .withTtl(300)
                    .withSound(sound).build(),
                key
            );

        log.info(pushoverResponse.toString());
        assertEquals(1, pushoverResponse.status());
    }

    public static void main(String[] args) {
        new EncryptedPushoverCall().simpleTest();
    }
}
