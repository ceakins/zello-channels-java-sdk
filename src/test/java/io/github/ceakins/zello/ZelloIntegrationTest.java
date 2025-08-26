package io.github.ceakins.zello;

import io.github.ceakins.zello.events.ZelloChannelListener;
import io.github.ceakins.zello.model.events.OnImageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ZelloIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ZelloIntegrationTest.class);

    private ZelloChannel channel;
    private ZelloChannelConfig config;
    private final AtomicReference<Throwable> listenerError = new AtomicReference<>();

    @BeforeClass
    public void setup() {
        String serverUrl = System.getProperty("zello.serverUrl");
        String username = System.getProperty("zello.username");
        String password = System.getProperty("zello.password");
        String channelName = System.getProperty("zello.channel");

        if (serverUrl == null || username == null || password == null || channelName == null) {
            throw new SkipException("Skipping integration tests: Zello credentials are not provided.");
        }

        config = ZelloChannelConfig.builder()
                .serverUrl(serverUrl)
                .username(username)
                .password(password)
                .channel(channelName)
                .build();
    }

    @AfterClass
    public void teardown() {
        if (channel != null) {
            logger.info("Tearing down: Disconnecting from channel...");
            channel.disconnect();
        }
    }

    @BeforeMethod
    public void beforeTest() throws Exception {
        listenerError.set(null);

        if (channel == null || channel.getState() != ConnectionState.CONNECTED) {
            if (channel != null) {
                channel.disconnect();
            }
            channel = new ZelloChannel(config);
            CountDownLatch connectionLatch = new CountDownLatch(1);

            channel.setListener(new ZelloChannelListener() {
                @Override public void onConnected() { connectionLatch.countDown(); }
                @Override public void onDisconnected(String reason) {}
                @Override public void onError(String errorMessage, Throwable t) {
                    listenerError.set(new AssertionError("Failed to connect: " + errorMessage, t));
                    connectionLatch.countDown();
                }
                @Override public void onTextMessage(String from, String message) {}
                @Override public void onStreamStarted(int streamId, String from) {}
                @Override public void onStreamStopped(int streamId, String from) {}
                @Override public void onAudioData(int streamId, byte[] audioData) {}
                @Override public void onImageEvent(OnImageEvent event) {}
            });

            channel.connect();
            Assert.assertTrue(connectionLatch.await(15, TimeUnit.SECONDS), "Failed to connect within 15 seconds.");
            if (listenerError.get() != null) Assert.fail("Test setup failed during connection.", listenerError.get());
        }
    }

    @Test(description = "Tests the basic ability to connect and disconnect without errors.")
    public void testConnectAndDisconnectSuccessfully() {
        logger.info("Running test: testConnectAndDisconnectSuccessfully");
        Assert.assertEquals(channel.getState(), ConnectionState.CONNECTED);
        logger.info("Connection successful. Test passed.");
    }

    @Test(description = "Tests sending a text message and receiving a success acknowledgement.")
    public void testSendTextMessageAndReceiveAck() throws InterruptedException {
        logger.info("Running test: testSendTextMessageAndReceiveAck");

        CountDownLatch ackLatch = new CountDownLatch(1);
        String uniqueMessage = "test-message-" + UUID.randomUUID();
        AtomicBoolean success = new AtomicBoolean(false);

        channel.setListener(new ZelloChannelListener() {
            @Override public void onConnected() {}
            @Override public void onDisconnected(String reason) {}
            @Override public void onError(String errorMessage, Throwable t) { listenerError.set(new AssertionError(errorMessage, t)); }
            @Override public void onStreamStarted(int streamId, String from) {}
            @Override public void onStreamStopped(int streamId, String from) {}
            @Override public void onAudioData(int streamId, byte[] audioData) {}
            @Override public void onImageEvent(OnImageEvent event) {}
            @Override public void onTextMessage(String from, String message) {}
        });

        logger.info("Sending unique text message: {}", uniqueMessage);
        channel.sendTextMessage(uniqueMessage, (response) -> {
            logger.info("Received ACK for text message: {}", response);
            if (response.optBoolean("success", false)) {
                success.set(true);
            }
            ackLatch.countDown();
        });

        Assert.assertTrue(ackLatch.await(10, TimeUnit.SECONDS), "Did not receive an acknowledgement for the text message within 10 seconds.");
        Assert.assertTrue(success.get(), "Server did not acknowledge the text message as successful.");
        if (listenerError.get() != null) Assert.fail("Test failed due to an error.", listenerError.get());
        logger.info("Successfully sent text message and received success acknowledgement.");
    }

    //@Test //I have to dig more into sending images.
    public void testSendImage() throws Exception {
        logger.info("Running test: testSendImage");
        String imageName = "sample_image.jpg";
        URL resourceUrl = getClass().getClassLoader().getResource(imageName);
        Assert.assertNotNull(resourceUrl, "Test image file '" + imageName + "' not found in src/test/resources");
        logger.info("Found test image file: {}", resourceUrl.getPath());

        byte[] imageData = resourceUrl.openStream().readAllBytes();
        Assert.assertTrue(imageData.length > 0, "Test image file is empty.");

        channel.setListener(new ZelloChannelListener() {
            @Override public void onConnected() {}
            @Override public void onDisconnected(String reason) {}
            @Override public void onError(String errorMessage, Throwable t) {
                listenerError.set(new AssertionError(errorMessage, t));
            }
            @Override public void onTextMessage(String from, String message) {}
            @Override public void onStreamStarted(int streamId, String from) {}
            @Override public void onStreamStopped(int streamId, String from) {}
            @Override public void onAudioData(int streamId, byte[] audioData) {}
            @Override public void onImageEvent(OnImageEvent event) {}
        });

        logger.info("Sending image...");
        channel.sendImage(imageData);

        Thread.sleep(5000);

        if (listenerError.get() != null) {
            Assert.fail("Test failed because an error was received after sending the image.", listenerError.get());
        }
        logger.info("Image sent successfully without any errors from the server.");
    }

}