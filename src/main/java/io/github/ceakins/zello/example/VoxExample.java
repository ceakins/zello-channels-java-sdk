package io.github.ceakins.zello.example;

import io.github.ceakins.zello.*;
import io.github.ceakins.zello.events.ZelloChannelListener;
import io.github.ceakins.zello.model.events.OnImageEvent;
import io.github.ceakins.zello.util.AudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.Mixer;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An example application demonstrating a real-time, voice-activated (VOX) client.
 * <p>
 * This class uses the high-level {@link ZelloRadioBridge} to manage all audio buffering,
 * threading, and VOX logic automatically. It also demonstrates selecting specific
 * input and output audio devices.
 */
public class VoxExample {

    private static final Logger logger = LoggerFactory.getLogger(VoxExample.class);

    public static void main(String[] args) throws Exception {
        final boolean PLAY_INCOMING_AUDIO = true;

        // --- Step 1: Configuration ---
        ZelloChannelConfig config = ZelloChannelConfig.builder()
                .serverUrl(System.getProperty("zello.serverUrl"))
                .username(System.getProperty("zello.username"))
                .password(System.getProperty("zello.password"))
                .channel(System.getProperty("zello.channel"))
                .build();

        if (config.getServerUrl() == null || config.getUsername() == null || config.getPassword() == null || config.getChannel() == null) {
            logger.error("Error: Please provide Zello credentials as system properties.");
            return;
        }

        Scanner scanner = new Scanner(System.in);

        // --- Step 2: Select Input Device (Microphone) ---
        AudioUtils.listAvailableMicrophones();
        System.out.print("\nEnter the number of the microphone you want to use for VOX: ");
        int micIndex = scanner.nextInt();
        scanner.nextLine(); // Consume the newline character
        Mixer.Info selectedMic = AudioUtils.getMicrophone(micIndex);

        if (selectedMic == null) {
            logger.error("Invalid microphone selected. Exiting.");
            scanner.close();
            return;
        }
        logger.info("Using microphone: {}", selectedMic.getName());

        // --- Step 3: Select Output Device (Speakers) ---
        Mixer.Info selectedSpeaker = null;
        if (PLAY_INCOMING_AUDIO) {
            AudioUtils.listAvailableSpeakers();
            System.out.print("\nEnter the number of the speaker you want to use for playback: ");
            int speakerIndex = scanner.nextInt();
            scanner.nextLine();
            selectedSpeaker = AudioUtils.getSpeaker(speakerIndex);
            if (selectedSpeaker == null) {
                logger.error("Invalid speaker selected. Exiting.");
                scanner.close();
                return;
            }
            logger.info("Using speaker: {}", selectedSpeaker.getName());
        }

        // --- Step 4: Initialize Managers and ZelloChannel ---
        ZelloChannel channel = new ZelloChannel(config);
        AudioOutputManager audioOutput = null;
        if (PLAY_INCOMING_AUDIO) {
            try {
                audioOutput = new AudioOutputManager(selectedSpeaker);
                audioOutput.start();
            } catch (Exception e) {
                logger.error("Failed to initialize audio output.", e);
            }
        }
        final AudioOutputManager finalAudioOutput = audioOutput;

        CountDownLatch connectionLatch = new CountDownLatch(1);
        channel.setListener(new ZelloChannelListener() {
            @Override public void onConnected() { logger.info("EVENT: Connected to channel!"); connectionLatch.countDown(); }
            @Override public void onDisconnected(String reason) { logger.info("EVENT: Disconnected. Reason: {}", reason); }
            @Override public void onError(String errorMessage, Throwable t) { logger.error("EVENT: Error: {}", errorMessage, t); connectionLatch.countDown(); }
            @Override public void onTextMessage(String from, String message) { logger.info("MSG: [{}] {}", from, message); }
            @Override public void onStreamStarted(int streamId, String from) { logger.info("AUDIO: Stream started from {}", from); }
            @Override public void onImageEvent(OnImageEvent event) { logger.info("IMAGE: Received image from {}", event.getFrom()); }

            @Override
            public void onStreamStopped(int streamId, String from) {
                logger.info("AUDIO: Stream stopped from {}", from);
                if (PLAY_INCOMING_AUDIO && finalAudioOutput != null) {
                    finalAudioOutput.flush();
                }
            }

            @Override
            public void onAudioData(int streamId, byte[] audioData) {
                if (PLAY_INCOMING_AUDIO && finalAudioOutput != null) {
                    finalAudioOutput.playAudio(audioData);
                }
            }
        });

        channel.connect();
        if (!connectionLatch.await(15, TimeUnit.SECONDS)) {
            logger.error("Failed to connect to Zello within the time limit.");
            return;
        }

        // --- Step 5: Create and Start the Bridge ---
        ZelloRadioBridgeConfig bridgeConfig = ZelloRadioBridgeConfig.builder().build(); // Use default settings
        ZelloRadioBridge bridge = new ZelloRadioBridge(channel, selectedMic, bridgeConfig);
        bridge.start();

        // --- Step 6: Keep the application alive ---
        System.out.println("\nBridge is running. Speak into the microphone to transmit. Press ENTER to stop.");
        scanner.nextLine();

        // --- Step 7: Clean Shutdown ---
        bridge.stop();
        if (finalAudioOutput != null) {
            finalAudioOutput.stop();
        }
        channel.disconnect();
        scanner.close();
        logger.info("Application shut down.");
    }
}