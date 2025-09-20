package io.github.ceakins.zello;

import io.github.ceakins.zello.events.ZelloChannelListener;
import io.github.ceakins.zello.internal.audio.AudioConstants;
import io.github.ceakins.zello.model.events.OnImageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ZelloAudioStreamIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ZelloAudioStreamIntegrationTest.class);

    private ZelloChannel channel;
    private final AtomicReference<Throwable> listenerError = new AtomicReference<>();

    @BeforeClass
    public void setup() {
        String serverUrl = System.getProperty("zello.serverUrl");
        String username = System.getProperty("zello.username");
        String password = System.getProperty("zello.password");
        String channelName = System.getProperty("zello.channel");
        if (serverUrl == null || username == null || password == null || channelName == null) {
            throw new SkipException("Skipping audio stream tests: Zello credentials are not provided.");
        }
        ZelloChannelConfig config = ZelloChannelConfig.builder().serverUrl(serverUrl).username(username).password(password).channel(channelName).build();
        this.channel = new ZelloChannel(config);
    }

    @AfterClass
    public void teardown() {
        if (channel != null) {
            channel.disconnect();
        }
    }

    @Test(timeOut = 45000)
    public void testSendAudioFileStream() throws Exception {
        logger.info("Running test: testSendAudioFileStream");
        listenerError.set(null);

        CountDownLatch connectionLatch = new CountDownLatch(1);
        channel.setListener(new ZelloChannelListener() {
            @Override public void onConnected() { connectionLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onError(String errorMessage, Throwable t) {
                listenerError.set(new AssertionError("An unexpected error occurred: " + errorMessage, t));
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
        if (listenerError.get() != null) Assert.fail("Test failed due to an error during connection.", listenerError.get());

        logger.info("Connection established, preparing to stream audio...");

        String audioFileName = "sample_audio.raw";
        URL resourceUrl = getClass().getClassLoader().getResource(audioFileName);
        Assert.assertNotNull(resourceUrl, "Test audio file '" + audioFileName + "' not found in src/test/resources");
        logger.info("Found test audio file: {}", resourceUrl.getPath());

        byte[] allAudioBytes = resourceUrl.openStream().readAllBytes();
        Assert.assertTrue(allAudioBytes.length > 0, "Audio file is empty.");
        Assert.assertEquals(allAudioBytes.length % AudioConstants.FRAME_SIZE_BYTES, 0, "Audio file size must be an exact multiple of the frame size (" + AudioConstants.FRAME_SIZE_BYTES + " bytes).");

        logger.info("Starting voice stream...");
        channel.startVoiceStream();
        Thread.sleep(500);

        byte[] buffer = new byte[AudioConstants.FRAME_SIZE_BYTES];
        int frameCount = 0;
        long frameInterval = AudioConstants.FRAME_DURATION_MS;

        for (int i = 0; i < allAudioBytes.length; i += AudioConstants.FRAME_SIZE_BYTES) {
            if (listenerError.get() != null) { Assert.fail("Test failed because an error was received mid-stream.", listenerError.get()); }
            System.arraycopy(allAudioBytes, i, buffer, 0, AudioConstants.FRAME_SIZE_BYTES);
            channel.sendVoiceData(buffer);
            frameCount++;
            Thread.sleep(frameInterval);
        }

        logger.info("Stopping voice stream after sending {} frames.", frameCount);
        channel.stopVoiceStream();
        Thread.sleep(500);

        if (listenerError.get() != null) {
            Assert.fail("Test failed because an error was received.", listenerError.get());
        }
        logger.info("Audio stream test completed successfully.");
    }
}