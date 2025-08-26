package io.github.ceakins.zello.example;

import io.github.ceakins.zello.AudioInputManager;
import io.github.ceakins.zello.AudioOutputManager;
import io.github.ceakins.zello.ZelloChannel;
import io.github.ceakins.zello.ZelloChannelConfig;
import io.github.ceakins.zello.events.ZelloChannelListener;
import io.github.ceakins.zello.model.events.OnImageEvent;
import io.github.ceakins.zello.util.AudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.Mixer;
import java.util.Scanner;

public class PushToTalkExample {

    private static final Logger logger = LoggerFactory.getLogger(PushToTalkExample.class);

    public static void main(String[] args) throws Exception {
        final boolean PLAY_INCOMING_AUDIO = true;

        ZelloChannelConfig config = ZelloChannelConfig.builder()
                .serverUrl(System.getProperty("zello.serverUrl"))
                .username(System.getProperty("zello.username"))
                .password(System.getProperty("zello.password"))
                .channel(System.getProperty("zello.channel"))
                .build();

        if (config.getServerUrl() == null || config.getUsername() == null || config.getPassword() == null || config.getChannel() == null) {
            logger.error("Error: Please provide all required credentials via system properties.");
            return;
        }

        ZelloChannel channel = new ZelloChannel(config);
        Scanner scanner = new Scanner(System.in);

        AudioUtils.listAvailableMicrophones();
        System.out.print("\nEnter the number of the microphone you want to use: ");
        int micIndex = scanner.nextInt();
        scanner.nextLine();
        Mixer.Info selectedMic = AudioUtils.getMicrophone(micIndex);
        if (selectedMic == null) {
            logger.error("Invalid microphone selected. Exiting.");
            return;
        }
        logger.info("Using microphone: {}", selectedMic.getName());

        Mixer.Info selectedSpeaker = null;
        if (PLAY_INCOMING_AUDIO) {
            AudioUtils.listAvailableSpeakers();
            System.out.print("\nEnter the number of the speaker you want to use: ");
            int speakerIndex = scanner.nextInt();
            scanner.nextLine();
            selectedSpeaker = AudioUtils.getSpeaker(speakerIndex);
            if (selectedSpeaker == null) {
                logger.error("Invalid speaker selected. Exiting.");
                return;
            }
            logger.info("Using speaker: {}", selectedSpeaker.getName());
        }

        AudioInputManager audioInput = new AudioInputManager(selectedMic, channel::sendVoiceData);
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

        channel.setListener(new ZelloChannelListener() {
            @Override public void onConnected() { logger.info("EVENT: Connected to channel!"); }
            @Override public void onDisconnected(String reason) { logger.info("EVENT: Disconnected. Reason: {}", reason); }
            @Override public void onError(String errorMessage, Throwable t) { logger.error("EVENT: Error: {}", errorMessage, t); }
            @Override public void onTextMessage(String from, String message) { logger.info("MSG: [{}] {}", from, message); }
            @Override public void onStreamStarted(int streamId, String from) { logger.info("AUDIO: Stream started from {}", from); }
            @Override public void onImageEvent(OnImageEvent event) { logger.info("IMAGE: Received image from {}", event.getFrom()); }

            @Override
            public void onStreamStopped(int streamId, String from) {
                logger.info("AUDIO: Stream stopped from {}", from);
                // --- THIS IS THE CRITICAL FIX ---
                // Flush the speaker buffer to discard any lingering audio and prevent looping.
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

        System.out.println("\nConnected. Press ENTER to start talking, and ENTER again to stop.");
        System.out.println("Type 'exit' to quit.");

        while (true) {
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input)) {
                break;
            }
            logger.info(">>> Transmitting... Press ENTER to stop. <<<");
            channel.startVoiceStream();
            audioInput.start();

            scanner.nextLine();

            logger.info(">>> Transmission stopped. <<<");
            audioInput.stop();
            channel.stopVoiceStream();
        }

        logger.info("Exiting...");
        if (finalAudioOutput != null) {
            finalAudioOutput.stop();
        }
        channel.disconnect();
        scanner.close();
    }
}