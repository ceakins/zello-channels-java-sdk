package io.github.ceakins.zello.example;

import io.github.ceakins.zello.ZelloChannel;
import io.github.ceakins.zello.ZelloChannelConfig;
import io.github.ceakins.zello.ZelloRadioBridge;
import io.github.ceakins.zello.ZelloRadioBridgeConfig;
import io.github.ceakins.zello.events.ZelloChannelListener;
import io.github.ceakins.zello.model.events.OnImageEvent;
import io.github.ceakins.zello.util.AudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.Mixer;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class VoxExample {

    private static final Logger logger = LoggerFactory.getLogger(VoxExample.class);

    public static void main(String[] args) throws Exception {
        // ... (ZelloChannelConfig setup is the same)
        ZelloChannelConfig config = ZelloChannelConfig.builder()
                .serverUrl(System.getProperty("zello.serverUrl"))
                .username(System.getProperty("zello.username"))
                .password(System.getProperty("zello.password"))
                .channel(System.getProperty("zello.channel"))
                .build();
        if (config.getServerUrl() == null) {
            logger.error("Error: Please provide Zello credentials as system properties.");
            return;
        }

        // ... (Microphone selection is the same)
        AudioUtils.listAvailableMicrophones();
        System.out.print("\nEnter the number of the microphone you want to use for VOX: ");
        Scanner scanner = new Scanner(System.in);
        int micIndex = scanner.nextInt();
        scanner.nextLine();
        Mixer.Info selectedMic = AudioUtils.getMicrophone(micIndex);
        if (selectedMic == null) {
            logger.error("Invalid microphone selected. Exiting.");
            return;
        }
        logger.info("Using microphone: {}", selectedMic.getName());

        // --- Step 3: Configure and create the bridge ---

        // Option 1: Use the default VOX settings
        //ZelloRadioBridgeConfig bridgeConfig = ZelloRadioBridgeConfig.builder().build();

        // Option 2: Customize the VOX settings for higher sensitivity
         ZelloRadioBridgeConfig bridgeConfig = ZelloRadioBridgeConfig.builder()
                 .voxOpenThreshold(0.02)  // Lower threshold = more sensitive
                 .voxCloseThreshold(0.01)
                 .voxHangTimeMs(1500)     // Longer delay before closing
                 .build();

        // ... (ZelloChannel setup and connection is the same)
        ZelloChannel channel = new ZelloChannel(config);
        CountDownLatch connectionLatch = new CountDownLatch(1);
        channel.setListener(new ZelloChannelListener() {
            @Override public void onConnected() { logger.info("EVENT: Connected to channel!"); connectionLatch.countDown(); }
            @Override public void onDisconnected(String reason) { logger.info("EVENT: Disconnected. Reason: {}", reason); }
            @Override public void onError(String errorMessage, Throwable t) { logger.error("EVENT: Error: {}", errorMessage, t); connectionLatch.countDown(); }
            @Override public void onTextMessage(String from, String message) { logger.info("MSG: [{}] {}", from, message); }
            @Override public void onStreamStarted(int streamId, String from) { logger.info("AUDIO: Stream started from {}", from); }
            @Override public void onStreamStopped(int streamId, String from) { logger.info("AUDIO: Stream stopped from {}", from); }
            @Override public void onAudioData(int streamId, byte[] audioData) {}
            @Override public void onImageEvent(OnImageEvent event) {}
        });
        channel.connect();
        if (!connectionLatch.await(15, TimeUnit.SECONDS)) {
            logger.error("Failed to connect to Zello within the time limit.");
            return;
        }

        // --- Step 4: Create and start the bridge with the chosen configuration ---
        ZelloRadioBridge bridge = new ZelloRadioBridge(channel, selectedMic, bridgeConfig);
        bridge.start();

        // --- Step 5: Keep the application alive ---
        System.out.println("\nBridge is running. Speak into the microphone to transmit. Press ENTER to stop.");
        scanner.nextLine();

        // --- Step 6: Clean shutdown ---
        bridge.stop();
        channel.disconnect();
        scanner.close();
        logger.info("Application shut down.");
    }
}