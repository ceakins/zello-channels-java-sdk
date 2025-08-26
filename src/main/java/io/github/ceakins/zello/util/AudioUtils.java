package io.github.ceakins.zello.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

/**
 * Utility class for audio-related helper functions.
 */
public final class AudioUtils {

    private static final Logger logger = LoggerFactory.getLogger(AudioUtils.class);

    private AudioUtils() {}

    /**
     * Lists all available audio input devices (microphones) to the INFO log.
     */
    public static void listAvailableMicrophones() {
        logger.info("Available Audio Input Devices (Microphones):");
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        int micIndex = 0;
        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            // A mixer is an input device if it has target lines.
            if (mixer.getTargetLineInfo().length > 0) {
                logger.info("  [{}] {}", micIndex, mixerInfo.getName());
                micIndex++;
            }
        }
    }

    /**
     * Finds a microphone mixer by its index, as listed by listAvailableMicrophones.
     * @param index The index of the mixer.
     * @return The Mixer.Info, or null if not found or invalid.
     */
    public static Mixer.Info getMicrophone(int index) {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        int currentIndex = 0;
        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.getTargetLineInfo().length > 0) {
                if (currentIndex == index) {
                    return mixerInfo;
                }
                currentIndex++;
            }
        }
        return null;
    }

    /**
     * Lists all available audio output devices (speakers) to the INFO log.
     */
    public static void listAvailableSpeakers() {
        logger.info("Available Audio Output Devices (Speakers):");
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        int speakerIndex = 0;
        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            // A mixer is an output device if it has source lines.
            if (mixer.getSourceLineInfo().length > 0) {
                logger.info("  [{}] {}", speakerIndex, mixerInfo.getName());
                speakerIndex++;
            }
        }
    }

    /**
     * Finds a speaker mixer by its index, as listed by listAvailableSpeakers.
     * @param index The index of the mixer.
     * @return The Mixer.Info, or null if not found or invalid.
     */
    public static Mixer.Info getSpeaker(int index) {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        int currentIndex = 0;
        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.getSourceLineInfo().length > 0) {
                if (currentIndex == index) {
                    return mixerInfo;
                }
                currentIndex++;
            }
        }
        return null;
    }

}