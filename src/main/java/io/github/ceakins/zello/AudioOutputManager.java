package io.github.ceakins.zello;

import io.github.ceakins.zello.internal.audio.AudioConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;

/**
 * Manages the playback of incoming audio to a specified or default speaker device.
 * It opens an audio line and provides a simple method to write PCM data to it.
 */
public class AudioOutputManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AudioOutputManager.class);

    private final Mixer.Info mixerInfo;
    private SourceDataLine speakers;

    public AudioOutputManager() {
        this(null);
    }

    public AudioOutputManager(Mixer.Info mixerInfo) {
        this.mixerInfo = mixerInfo;
    }

    public void start() throws LineUnavailableException {
        if (speakers != null && speakers.isOpen()) {
            return;
        }

        AudioFormat format = new AudioFormat(AudioConstants.SAMPLE_RATE, 16, AudioConstants.CHANNELS, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        if (mixerInfo != null) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            speakers = (SourceDataLine) mixer.getLine(info);
            logger.info("Opening specified speaker line: {}", mixerInfo.getName());
        } else {
            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Default speaker line for " + format + " is not supported.");
            }
            speakers = (SourceDataLine) AudioSystem.getLine(info);
            logger.info("Opening default system speaker line.");
        }

        speakers.open(format);
        speakers.start();
        logger.info("Speaker line started successfully.");
    }

    public void stop() {
        if (speakers != null && speakers.isOpen()) {
            logger.info("Closing speaker line...");
            speakers.drain();
            speakers.stop();
            speakers.close();
            logger.info("Speaker line closed.");
        }
    }

    public void playAudio(byte[] pcmData) {
        if (speakers != null && speakers.isOpen()) {
            speakers.write(pcmData, 0, pcmData.length);
        }
    }

    /**
     * Clears any queued audio data from the speaker line's internal buffer.
     * This should be called when an incoming audio stream stops to prevent audio looping.
     */
    public void flush() {
        if (speakers != null && speakers.isOpen()) {
            speakers.flush();
        }
    }

    @Override
    public void close() {
        stop();
    }
}