package de.kleinkop.pushover4j.manual;

import de.kleinkop.pushover4j.PushoverClient;
import de.kleinkop.pushover4j.TestUtils;
import de.kleinkop.pushover4j.http.PushoverHttpClient;

import java.io.File;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

abstract public class ManualPushoverCall {
    final private PushoverClient pushoverClient;

    final private PushoverClient failingPushoverClient;

    protected String device = System.getenv("PUSHOVER_DEVICE");
    protected String sound = System.getenv("PUSHOVER_SOUND");

    public ManualPushoverCall() {
        pushoverClient = new PushoverHttpClient(
            System.getenv("PUSHOVER_TOKEN"),
            System.getenv("PUSHOVER_USER")
        );
        failingPushoverClient = new PushoverHttpClient(
            "wrongtoken",
            "wronguser"
        );
    }

    protected PushoverClient pushover() {
        return pushoverClient;
    }

    protected PushoverClient failingPushover() {
        return failingPushoverClient;
    }

    protected File file(String filename) {
        return new File(TestUtils.getResource(filename).getFile());
    }

    protected void waiting(Long timeInSeconds) {
        await("waiting-on-pushover").pollDelay(timeInSeconds, SECONDS).until(() -> true);
    }
}
