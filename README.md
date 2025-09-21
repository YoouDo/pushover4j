# Pushover4k

![master](https://github.com/YoouDo/pushover4j/actions/workflows/master.yaml/badge.svg)

### A Pushover client in Java

A simple library to use [Pushover](https://www.pushover.net) messages in JVM applications.

Use the following dependency in your application:
```kotlin
implementation("de.kleinkop:pushover4j:1.0.0")
```

### Changes
#### Release 1.0.0
Initial release

### Examples:

Create a client using your Pushover tokens:

```java
var pushoverClient = new PushoverHttpClient(
    "your app token",
    "your user token",
    3,                                       // number of retries, default: 5
    6000L                                    // retry interval in millis, default: 5000L
);
```

Send a simple message with default values:

```java
var response = pushoverClient.sendMessage(
    Message.of("This is a test message").build()
);
```

Send a message using all available options excluding those for emergency messages:

```java
var response = pushoverClient.sendMessage(
    Message.of("This is another test message")
        .withTitle("A title")
        .withPriority(Priority.HIGH)
        .withUrl("https://www.pushover.net")
        .withUrlTitle("Pushover")
        .withDevice("device1")
        .withDevice("device2")
        .withTimestamp(OffsetDateTime.now())
        .withHtml(true)
        .withSound("magic")
        .withImage(File("image.png"))
        .withMonospace(false)
        .build()
    );
```

Send an emergency message:
```java
    var response = pushover().sendMessage(
    Message.of("Testing emergency")
            .withTitle("Emergency test")
            .withPriority(Priority.EMERGENCY)
            .withDevice("device")
            .withTag("aTag")                   // optional: tags for emergency message
            .withTag("anotherTAG")
            .withRetry(30)                     // required: number of retries from Pushover server
            .withExpiration(120)               // required: time to live of emergency message
            .build()
    );
```
Please note that you have to provide values for `retry` and `expire`. Usings `tags` is optional.

---

All properties of `Message`:

| property  | type           |      optional      | description                                                                           |
|-----------|----------------|:------------------:|---------------------------------------------------------------------------------------|
| message   | String         |                    | The only mandatory parameter                                                          |
| title     | String         | :heavy_check_mark: | If not provided the Pushover default will be used                                     |
| priority  | Priority       | :heavy_check_mark: | Priority as defined by [Pushover](https://pushover.net/api#priority)                  |
| url       | String         | :heavy_check_mark: | Will be shown as supplementary URL in the message                                     |
| urlTitle  | String         | :heavy_check_mark: | Supplementary URL will be shown with this title                                       |
| devices   | List<String>   | :heavy_check_mark: | Message will be sent to these devices only                                            |
| timestamp | OffsetDateTime | :heavy_check_mark: | This time will be used as a message time                                              |
| html      | Boolean        | :heavy_check_mark: | Use simple HTML tags in the message                                                   |
| sound     | String         | :heavy_check_mark: | Client device will use this sound.                                                    |
| image     | File           | :heavy_check_mark: | Image will be added to message                                                        |
| monospace | Boolean        | :heavy_check_mark: | Message will be rendered with monospace font. Only useable in non-html messages       |
| retry     | Int            | :heavy_check_mark: | Emergency messages will be retried with this interval in seconds. Minimum value is 30 |
| expire    | Int            | :heavy_check_mark: | Emergency message will expire after this period in seconds                            |
| tags      | List<String>   | :heavy_check_mark: | Tags to be added to emergency message. May be used for cancellations                  |
| ttl       | Int            | :heavy_check_mark: | Number of seconds that the message will live, before being deleted automatically      |

#### Response object
Sending a message will return information provided by Pushover using following data class:

```java
public record PushoverResponse(
        int status,
        String request,
        String user,
        List<String> errors,
        String receipt,
        Integer canceled,
        ApplicationUsage applicationUsage
) {}
```
 
---
#### Emergency messages

Emergency messages can be queried and cancelled like this:
 ```java
 var response = pushoverClient.getEmergencyState("receipt-id of emergency message");
 ```

Cancel by receipt id:
```java
var resposne = pushoverClient.cancelEmergencyMessage("receipt-id of emergency message");
```

Cancel by tag with name `TAG`:
```java
var response = pushoverClient.cancelEmergencyMessageByTag("TAG");
```
