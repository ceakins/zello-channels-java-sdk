package io.github.ceakins.zello.internal.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Unit test for the AudioEngine.
 * This test does not require a network connection.
 */
public class AudioEngineTest {

    private static final Logger logger = LoggerFactory.getLogger(AudioEngineTest.class);

    private AudioEngine audioEngine;

    @BeforeClass
    public void setup() {
        audioEngine = new AudioEngine();
    }

    @AfterClass
    public void teardown() {
        if (audioEngine != null) {
            audioEngine.close();
        }
    }

    @Test
    public void testEncodeDecodeLoopback() {
        logger.info("Running test: testEncodeDecodeLoopback");

        // Arrange
        byte[] originalPcmData = new byte[AudioConstants.FRAME_SIZE_BYTES];

        // Act
        byte[] opusData = audioEngine.encode(originalPcmData);

        // Assert
        Assert.assertNotNull(opusData, "Encoded Opus data should not be null.");
        Assert.assertTrue(opusData.length > 0, "Encoded Opus data should not be empty.");
        logger.info("Encoded {} PCM bytes into {} Opus bytes.", originalPcmData.length, opusData.length);

        // Arrange for decoding
        int testStreamId = 1;
        audioEngine.startDecodingSession(testStreamId);

        // Act
        byte[] decodedPcmData = audioEngine.decode(testStreamId, opusData);
        audioEngine.stopDecodingSession(testStreamId);

        // Assert
        Assert.assertNotNull(decodedPcmData, "Decoded PCM data should not be null.");
        Assert.assertEquals(decodedPcmData.length, originalPcmData.length, "Decoded PCM data length should match the original length.");
        logger.info("Loopback test successful. Decoded data has the correct length.");
    }

}