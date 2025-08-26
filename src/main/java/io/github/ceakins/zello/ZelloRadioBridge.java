package io.github.ceakins.zello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.Mixer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An advanced audio bridge that provides local buffering, a dedicated transmission thread,
 * and a Voice Activity Detection (VOX) system to manage a real-time audio connection
 * to a Zello channel.
 * <p>
 * This class orchestrates a {@link ZelloChannel} and an {@link AudioInputManager}
 * to create a resilient audio streaming solution.
 */
public class ZelloRadioBridge {

    private static final Logger logger = LoggerFactory.getLogger(ZelloRadioBridge.class);

    private enum VoxState {
        LISTENING,
        TRANSMITTING
    }

    private final ZelloChannel zelloChannel;
    private final AudioInputManager audioInputManager;
    private final ZelloRadioBridgeConfig config;
    private final ExecutorService transmissionExecutor = Executors.newSingleThreadExecutor();
    private final LinkedBlockingQueue<byte[]> audioBuffer = new LinkedBlockingQueue<>();
    private volatile boolean isRunning = false;
    private volatile VoxState voxState = VoxState.LISTENING;
    private volatile long lastSoundTime = 0;

    // --- Pre-roll buffer for smooth VOX activation ---
    private final LinkedList<byte[]> preRollBuffer = new LinkedList<>();

    /**
     * Constructs the radio bridge.
     *
     * @param zelloChannel The configured ZelloChannel instance to use for communication.
     * @param audioInputMixer The microphone/input device to capture audio from.
     * @param config The configuration for the bridge's behavior (e.g., VOX settings).
     */
    public ZelloRadioBridge(ZelloChannel zelloChannel, Mixer.Info audioInputMixer, ZelloRadioBridgeConfig config) {
        this.zelloChannel = zelloChannel;
        this.config = config;
        this.audioInputManager = new AudioInputManager(audioInputMixer, this::processIncomingAudio);
    }

    /**
     * Starts the bridge: begins listening to the microphone and starts the transmission thread.
     */
    public void start() {
        if (isRunning) {
            return;
        }
        logger.info("Starting Zello Radio Bridge...");
        isRunning = true;
        transmissionExecutor.submit(this::transmitLoop);
        audioInputManager.start();
        logger.info("Bridge started. VOX is now active.");
    }

    /**
     * Stops the bridge: stops listening to the microphone and shuts down the transmission thread.
     */
    public void stop() {
        logger.info("Stopping Zello Radio Bridge...");
        isRunning = false;
        audioInputManager.stop();
        transmissionExecutor.shutdownNow();
        if (zelloChannel.getState() == ConnectionState.CONNECTED && voxState == VoxState.TRANSMITTING) {
            zelloChannel.stopVoiceStream();
        }
        logger.info("Bridge stopped.");
    }

    /**
     * This is the "Producer" method, called by the AudioInputManager on its thread for every audio frame.
     * It contains the corrected VOX logic with a pre-roll buffer and proper data cloning.
     */
    private void processIncomingAudio(byte[] pcmData) {
        if (!isRunning) {
            return;
        }

        // Always work with a copy to prevent the shared buffer in AudioInputManager from being overwritten.
        byte[] pcmDataCopy = pcmData.clone();

        // Always maintain the pre-roll buffer
        preRollBuffer.add(pcmDataCopy);
        while (preRollBuffer.size() > config.getPreRollFrameCount()) {
            preRollBuffer.removeFirst();
        }

        double rms = calculateRms(pcmDataCopy);
        logger.trace("VOX RMS level: {}", rms);

        if (voxState == VoxState.LISTENING) {
            if (rms > config.getVoxOpenThreshold()) {
                // Sound detected, open the gate
                logger.info("VOX Opened (RMS: {})", String.format("%.4f", rms));
                voxState = VoxState.TRANSMITTING;
                zelloChannel.startVoiceStream();
                lastSoundTime = System.currentTimeMillis();

                // Immediately dump the entire pre-roll buffer to the transmission queue to send the start of the sound.
                for (byte[] frame : preRollBuffer) {
                    audioBuffer.offer(frame);
                }
                preRollBuffer.clear();
            }
        } else { // voxState is TRANSMITTING
            // While transmitting, add the safe copy of the audio directly to the main buffer
            audioBuffer.offer(pcmDataCopy);
            if (rms > config.getVoxCloseThreshold()) {
                // Update the time of the last sound
                lastSoundTime = System.currentTimeMillis();
            } else {
                // Check if the silence has exceeded the hang time
                if (System.currentTimeMillis() - lastSoundTime > config.getVoxHangTimeMs()) {
                    logger.info("VOX Closed (Timeout)");
                    voxState = VoxState.LISTENING;
                    zelloChannel.stopVoiceStream();
                    audioBuffer.clear(); // Clear any buffered silence
                }
            }
        }
    }

    /**
     * This is the "Consumer" method, running on its own dedicated thread.
     * It pulls audio from the buffer and sends it to Zello.
     */
    private void transmitLoop() {
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] pcmData = audioBuffer.take();
                if (zelloChannel.getState() == ConnectionState.CONNECTED) {
                    zelloChannel.sendVoiceData(pcmData);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Transmission thread interrupted.");
                break;
            }
        }
    }

    /**
     * Calculates the Root Mean Square (RMS) of a PCM audio frame.
     * This is used as a measure of loudness for the VOX system.
     */
    private double calculateRms(byte[] pcmData) {
        long sumOfSquares = 0;
        ByteBuffer buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < pcmData.length / 2; i++) {
            short sample = buffer.getShort();
            sumOfSquares += (long) sample * sample;
        }
        double meanSquare = (double) sumOfSquares / (pcmData.length / 2);
        return Math.sqrt(meanSquare) / 32768.0;
    }

}