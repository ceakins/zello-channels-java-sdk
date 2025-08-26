package io.github.ceakins.zello.internal.audio;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.opus.Opus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.util.opus.Opus.*;

public class AudioEngine implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AudioEngine.class);

    private final ConcurrentHashMap<Integer, Long> decoders = new ConcurrentHashMap<>();
    private final long encoder;

    public AudioEngine() {
        logger.debug("Initializing AudioEngine...");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            encoder = opus_encoder_create(AudioConstants.SAMPLE_RATE, AudioConstants.CHANNELS, OPUS_APPLICATION_VOIP, error);
            if (error.get(0) != OPUS_OK) {
                throw new IllegalStateException("Failed to create Opus encoder: " + opus_strerror(error.get(0)));
            }
        }
        logger.info("Opus encoder created successfully.");
    }

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

    public void stopDecodingSession(int streamId) {
        Long decoder = decoders.remove(streamId);
        if (decoder != null) {
            opus_decoder_destroy(decoder);
            logger.debug("Opus decoder destroyed for stream {}", streamId);
        }
    }

    public byte[] decode(int streamId, byte[] opusData) {
        Long decoder = decoders.get(streamId);
        if (decoder == null) {
            logger.warn("Received audio for stream {}, but no decoder exists.", streamId);
            return null;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer opusDataBuffer = stack.malloc(opusData.length);
            opusDataBuffer.put(opusData).flip();

            // --- CORRECTED: Allocate a buffer large enough for the MAXIMUM possible frame size ---
            ByteBuffer pcmBytesBuffer = stack.malloc(AudioConstants.MAX_DECODE_BUFFER_SIZE_BYTES);
            ShortBuffer pcmOutputBuffer = pcmBytesBuffer.asShortBuffer();

            // The opus_decode function will return the ACTUAL number of samples it decoded.
            int samplesDecoded = opus_decode(decoder, opusDataBuffer, pcmOutputBuffer, AudioConstants.MAX_SAMPLES_PER_PACKET, 0);
            if (samplesDecoded < 0) {
                logger.error("Opus decoding failed for stream {}: {}", streamId, opus_strerror(samplesDecoded));
                return null;
            }

            int bytesDecoded = samplesDecoded * AudioConstants.CHANNELS * 2;
            byte[] pcmBytes = new byte[bytesDecoded];

            pcmBytesBuffer.limit(bytesDecoded);
            pcmBytesBuffer.get(pcmBytes);

            return pcmBytes;
        }
    }

    public byte[] encode(byte[] pcmData) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pcmBytesBuffer = stack.malloc(pcmData.length);
            pcmBytesBuffer.put(pcmData);
            pcmBytesBuffer.flip();
            ShortBuffer pcmInputBuffer = pcmBytesBuffer.order(ByteOrder.nativeOrder()).asShortBuffer();
            ByteBuffer opusOutputBuffer = stack.malloc(AudioConstants.MAX_OPUS_PACKET_SIZE);
            int bytesEncoded = opus_encode(encoder, pcmInputBuffer, AudioConstants.SAMPLES_PER_FRAME, opusOutputBuffer);
            if (bytesEncoded < 0) {
                logger.error("Opus encoding failed: {}", opus_strerror(bytesEncoded));
                return null;
            }
            byte[] opusBytes = new byte[bytesEncoded];
            opusOutputBuffer.get(opusBytes);
            return opusBytes;
        }
    }

    @Override
    public void close() {
        logger.debug("Closing AudioEngine and destroying all active codecs...");
        opus_encoder_destroy(encoder);
        logger.info("Opus encoder destroyed.");
        decoders.values().forEach(Opus::opus_decoder_destroy);
        decoders.clear();
        logger.info("All active Opus decoders destroyed.");
    }

}