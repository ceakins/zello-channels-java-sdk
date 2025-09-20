package io.github.ceakins.zello.util;

import io.github.ceakins.zello.internal.audio.AudioConstants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A utility to generate the raw PCM audio file needed for integration tests.
 * Run the main method once to create the file.
 */
public class CreateTestAudioFile {

    public static void main(String[] args) {
        // --- Configuration ---
        double durationSeconds = 1.0;         // 1-second long tone
        double frequencyHz = 440.0;           // A4 note (standard test tone)
        double amplitude = 0.8;               // 80% of max volume to avoid clipping

        // --- File Path ---
        // This will place the file in 'src/test/resources/sample_audio.raw'
        Path outputPath = Paths.get("src", "test", "resources", "sample_audio.raw");

        System.out.println("Generating test audio file...");
        System.out.println("  Format: " + AudioConstants.SAMPLE_RATE + " Hz, 16-bit, Mono");
        System.out.println("  Duration: " + durationSeconds + "s");
        System.out.println("  Frequency: " + frequencyHz + " Hz");
        System.out.println("  Output Path: " + outputPath.toAbsolutePath());

        try {
            // Ensure the parent directory exists
            Files.createDirectories(outputPath.getParent());

            int numSamples = (int) (AudioConstants.SAMPLE_RATE * durationSeconds);
            // Each sample is a 16-bit short (2 bytes)
            ByteBuffer buffer = ByteBuffer.allocate(numSamples * 2);
            // Zello expects little-endian byte order for PCM data
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < numSamples; i++) {
                // Calculate the sine wave value for the current sample
                double sinValue = Math.sin(2 * Math.PI * frequencyHz * i / AudioConstants.SAMPLE_RATE);
                // Convert the double value [-1.0, 1.0] to a 16-bit short [-32767, 32767]
                short sample = (short) (sinValue * Short.MAX_VALUE * amplitude);
                buffer.putShort(sample);
            }

            Files.write(outputPath, buffer.array());
            System.out.println("\nSUCCESS: Test audio file 'sample_audio.raw' created successfully!");

        } catch (IOException e) {
            System.err.println("\nERROR: Failed to create test audio file.");
            e.printStackTrace();
        }
    }
}