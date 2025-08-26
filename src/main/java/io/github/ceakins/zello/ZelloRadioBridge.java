package io.github.ceakins.zello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.Mixer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An advanced audio bridge that provides local buffering, a dedicated transmission thread,
 * and a Voice Activity Detection (VOX) system to manage a real-time audio connection
 * to a Zello channel.
 */
public class ZelloRadioBridge {

    private static final Logger logger = LoggerFactory.getLogger(ZelloRadioBridge.class);

    private enum VoxState {
        LISTENING,
        TRANSMITTING
    }

    private final ZelloChannel zelloChannel;
    private final AudioInputManager audioInputManager;
    private final ZelloRadioBridgeConfig config; // <-- Replaces the static constants
    private final ExecutorService transmissionExecutor = Executors.newSingleThreadExecutor();
    private final LinkedBlockingQueue<byte[]> audioBuffer = new LinkedBlockingQueue<>();
    private volatile boolean isRunning = false;
    private volatile VoxState voxState = VoxState.LISTENING;
    private volatile long lastSoundTime = 0;

    /**
     * Constructs the radio bridge with a specific configuration.
     *
     * @param zelloChannel The configured ZelloChannel instance to use for communication.
     * @param audioInputMixer The microphone/input device to capture audio from.
     * @param config The configuration object for VOX and other bridge settings.
     */
    public ZelloRadioBridge(ZelloChannel zelloChannel, Mixer.Info audioInputMixer, ZelloRadioBridgeConfig config) {
        this.zelloChannel = zelloChannel;
        this.audioInputManager = new AudioInputManager(audioInputMixer, this::processIncomingAudio);
        this.config = config; // <-- Store the config
    }

    /**
     * Starts the bridge: begins listening to the microphone and starts the transmission thread.
     */
    public void start() {
        if (isRunning) {
            return;
        }
        logger.info("Starting Zello Radio Bridge with VOX open threshold of {}", config.getVoxOpenThreshold());
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

    private void processIncomingAudio(byte[] pcmData) {
        if (!isRunning) {
            return;
        }

        double rms = calculateRms(pcmData);
        logger.trace("VOX RMS level: {}", rms);

        // --- VOX State Machine (now uses the config object) ---
        if (voxState == VoxState.LISTENING) {
            if (rms > config.getVoxOpenThreshold()) {
                logger.info("VOX Opened (RMS: {})", String.format("%.4f", rms));
                voxState = VoxState.TRANSMITTING;
                zelloChannel.startVoiceStream();
                lastSoundTime = System.currentTimeMillis();
                audioBuffer.offer(pcmData);
            }
        } else { // voxState is TRANSMITTING
            audioBuffer.offer(pcmData);
            if (rms > config.getVoxCloseThreshold()) {
                lastSoundTime = System.currentTimeMillis();
            } else {
                if (System.currentTimeMillis() - lastSoundTime > config.getVoxHangTimeMs()) {
                    logger.info("VOX Closed (Timeout)");
                    voxState = VoxState.LISTENING;
                    zelloChannel.stopVoiceStream();
                    audioBuffer.clear();
                }
            }
        }
    }

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