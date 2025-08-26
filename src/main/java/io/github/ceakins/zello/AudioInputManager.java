package io.github.ceakins.zello;

import io.github.ceakins.zello.internal.audio.AudioConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.util.function.Consumer;

/**
 * Manages capturing audio from a specified input device (microphone) in real-time.
 * This class runs in its own thread and provides audio data in properly sized chunks
 * via a callback, ready to be sent to the ZelloChannel.
 */
public class AudioInputManager implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AudioInputManager.class);

    private final Mixer.Info mixerInfo;
    private final Consumer<byte[]> onAudioData;
    private volatile boolean isRunning = false;
    private TargetDataLine microphone;

    /**
     * Constructs an AudioInputManager.
     *
     * @param mixerInfo The info for the audio input device to use. Can be null for the system default.
     * @param onAudioData A callback that will be invoked with a properly sized PCM audio frame (640 bytes) every 20ms.
     */
    public AudioInputManager(Mixer.Info mixerInfo, Consumer<byte[]> onAudioData) {
        this.mixerInfo = mixerInfo;
        this.onAudioData = onAudioData;
    }

    /**
     * Starts capturing audio from the microphone. This method spawns a new thread.
     */
    public void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        Thread captureThread = new Thread(this);
        captureThread.setName("Zello-Audio-Capture-Thread");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Stops capturing audio from the microphone.
     */
    public void stop() {
        isRunning = false;
        if (microphone != null) {
            // These methods can be called from any thread and will signal the run() loop to exit.
            microphone.stop();
            microphone.close();
        }
    }

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(AudioConstants.SAMPLE_RATE, 16, AudioConstants.CHANNELS, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (mixerInfo != null) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                microphone = (TargetDataLine) mixer.getLine(info);
                logger.info("Opening microphone: {}", mixerInfo.getName());
            } else {
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                logger.info("Opening default system microphone.");
            }

            microphone.open(format);
            microphone.start();
            logger.info("Microphone capture started.");

            byte[] buffer = new byte[AudioConstants.FRAME_SIZE_BYTES];
            while (isRunning) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    onAudioData.accept(buffer);
                }
            }
        } catch (LineUnavailableException e) {
            logger.error("Audio line is unavailable. Another application may be using the microphone.", e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred in the audio capture thread.", e);
        } finally {
            if (microphone != null) {
                microphone.close();
            }
            isRunning = false;
            logger.info("Microphone capture stopped.");
        }
    }
}