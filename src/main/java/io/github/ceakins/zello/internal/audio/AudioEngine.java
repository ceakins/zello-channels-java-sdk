package io.github.ceakins.zello.internal.audio;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.util.opus.Opus.*;

/**
 * Manages all Opus audio encoding and decoding.
 * This class interfaces directly with the LWJGL Opus bindings.
 */
public class AudioEngine implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AudioEngine.class);

    // A map to hold active decoders, keyed by stream ID.
    private final ConcurrentHashMap<Integer, Long> decoders = new ConcurrentHashMap<>();

    // A single encoder for outgoing audio.
    private final long encoder;

    public AudioEngine() {
        logger.debug("Initializing AudioEngine...");
        // Create the encoder for outgoing audio.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            encoder = opus_encoder_create(AudioConstants.SAMPLE_RATE, AudioConstants.CHANNELS, OPUS_APPLICATION_VOIP, error);
            if (error.get(0) != OPUS_OK) {
                throw new IllegalStateException("Failed to create Opus encoder: " + opus_strerror(error.get(0)));
            }
        }
        logger.info("Opus encoder created successfully.");
    }

    /**
     * Creates and registers a new decoder for an incoming audio stream.
     *
     * @param streamId The ID of the incoming stream.
     */
    public void startDecodingSession(int streamId) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            long decoder = opus_decoder_create(AudioConstants.SAMPLE_RATE, AudioConstants.CHANNELS, error);
            if (error.get(0) != OPUS_OK) {
                logger.error("Failed to create Opus decoder for stream {}: {}", streamId, opus_strerror(error.get(0)));
                return;
            }
            decoders.put(streamId, decoder);
            logger.debug("Opus decoder created for stream {}", streamId);
        }
    }

    /**
     * Destroys the decoder associated with a finished audio stream.
     *
     * @param streamId The ID of the stream to stop.
     */
    public void stopDecodingSession(int streamId) {
        Long decoder = decoders.remove(streamId);
        if (decoder != null) {
            opus_decoder_destroy(decoder);
            logger.debug("Opus decoder destroyed for stream {}", streamId);
        }
    }

    /**
     * Decodes a raw Opus packet into PCM audio data.
     *
     * @param streamId The stream the packet belongs to.
     * @param opusData The compressed Opus data.
     * @return A byte array of raw 16-bit PCM data, or null if decoding fails.
     */
    public byte[] decode(int streamId, byte[] opusData) {
        Long decoder = decoders.get(streamId);
        if (decoder == null) {
            logger.warn("Received audio for stream {}, but no decoder exists.", streamId);
            return null;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Allocate a buffer for the compressed Opus data
            ByteBuffer opusDataBuffer = stack.malloc(opusData.length);
            opusDataBuffer.put(opusData).flip();

            // Allocate a buffer for the decoded PCM data
            ShortBuffer pcmOutputBuffer = stack.mallocShort(AudioConstants.SAMPLES_PER_FRAME * AudioConstants.CHANNELS);

            int samplesDecoded = opus_decode(decoder, opusDataBuffer, pcmOutputBuffer, AudioConstants.SAMPLES_PER_FRAME, 0);
            if (samplesDecoded < 0) {
                logger.error("Opus decoding failed for stream {}: {}", streamId, opus_strerror(samplesDecoded));
                return null;
            }

            // Copy the decoded PCM data from the ShortBuffer to a byte array
            byte[] pcmBytes = new byte[samplesDecoded * AudioConstants.CHANNELS * 2];
            // LWJGL's MemoryUtil provides an efficient way to do this copy
            MemoryUtil.memCopy(MemoryUtil.memAddress(pcmOutputBuffer), MemoryUtil.memAddress(ByteBuffer.wrap(pcmBytes)), pcmBytes.length);

            return pcmBytes;
        }
    }

    /**
     * Encodes a raw PCM audio frame into a compressed Opus packet.
     *
     * @param pcmData A byte array containing exactly one frame of raw 16-bit PCM data.
     * @return A byte array of compressed Opus data, or null if encoding fails.
     */
    public byte[] encode(byte[] pcmData) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Allocate a buffer and copy the PCM data into it
            ShortBuffer pcmInputBuffer = stack.mallocShort(pcmData.length / 2);
            MemoryUtil.memCopy(MemoryUtil.memAddress(ByteBuffer.wrap(pcmData)), MemoryUtil.memAddress(pcmInputBuffer), pcmData.length);

            // Allocate a buffer for the compressed Opus data output
            ByteBuffer opusOutputBuffer = stack.malloc(AudioConstants.MAX_OPUS_PACKET_SIZE);

            int bytesEncoded = opus_encode(encoder, pcmInputBuffer, AudioConstants.SAMPLES_PER_FRAME, opusOutputBuffer);
            if (bytesEncoded < 0) {
                logger.error("Opus encoding failed: {}", opus_strerror(bytesEncoded));
                return null;
            }

            // Copy the encoded data into a perfectly sized byte array
            byte[] opusBytes = new byte[bytesEncoded];
            opusOutputBuffer.get(opusBytes);
            return opusBytes;
        }
    }


    @Override
    public void close() {
        logger.debug("Closing AudioEngine and destroying all active codecs...");
        // Destroy the main encoder
        opus_encoder_destroy(encoder);
        logger.info("Opus encoder destroyed.");

        // Destroy all remaining decoders
        decoders.values().forEach(decoder -> {
            opus_decoder_destroy(decoder);
        });
        decoders.clear();
        logger.info("All active Opus decoders destroyed.");
    }

}