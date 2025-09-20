package io.github.ceakins.zello;

import io.github.ceakins.zello.events.ZelloChannelListener;
import io.github.ceakins.zello.internal.WebSocketClientFactory;
import io.github.ceakins.zello.internal.ZelloWebSocketClient;
import io.github.ceakins.zello.internal.audio.AudioEngine;
import io.github.ceakins.zello.model.events.OnChannelStatusEvent;
import io.github.ceakins.zello.model.events.OnTextMessageEvent;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ZelloChannelTest {

    @Mock
    private AudioEngine mockAudioEngine;
    @Mock
    private ZelloWebSocketClient mockWebSocketClient;
    @Mock
    private WebSocketClientFactory mockWebSocketFactory;
    @Mock
    private ZelloChannelListener mockListener;

    private ZelloChannel zelloChannel;
    private ZelloChannelConfig config;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        config = ZelloChannelConfig.builder().serverUrl("wss://test.zello.com/ws").username("testuser").password("testpass").channel("testchannel").build();

        when(mockWebSocketClient.isOpen()).thenReturn(true);
        when(mockWebSocketFactory.create(any(), any(), any())).thenReturn(mockWebSocketClient);

        zelloChannel = new ZelloChannel(config, mockAudioEngine, mockWebSocketFactory);
        zelloChannel.setListener(mockListener);
        zelloChannel.connect();
    }

    @Test
    public void testConnect_StartsWebSocket() {
        verify(mockWebSocketClient, times(1)).connect();
        assertEquals(zelloChannel.getState(), ConnectionState.CONNECTING);
    }

    @Test
    public void testOnOpen_SendsLogonCommand() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        zelloChannel.onOpen();

        verify(mockWebSocketClient, times(1)).send(captor.capture());
        String sentJson = captor.getValue();
        JSONObject json = new JSONObject(sentJson);

        assertEquals(zelloChannel.getState(), ConnectionState.LOGGING_IN);
        assertEquals(json.getString("command"), "logon");
    }

    @Test
    public void testOnServerCommand_ChannelStatusOnline_NotifiesListener() {
        OnChannelStatusEvent event = new OnChannelStatusEvent();
        event.setStatus("online");

        zelloChannel.onServerCommand(event);

        assertEquals(zelloChannel.getState(), ConnectionState.CONNECTED);
        verify(mockListener, times(1)).onConnected();
    }

    @Test
    public void testOnServerCommand_TextMessage_NotifiesListener() {
        OnTextMessageEvent event = new OnTextMessageEvent();
        event.setFrom("otheruser");
        event.setMessage("Hello");

        zelloChannel.onServerCommand(event);

        verify(mockListener, times(1)).onTextMessage("otheruser", "Hello");
    }

    @Test
    public void testSendTextMessage_SendsCorrectJson() {
        // --- THIS IS THE CRITICAL FIX ---
        // ARRANGE: First, simulate a successful connection to put the channel in the 'CONNECTED' state.
        OnChannelStatusEvent statusEvent = new OnChannelStatusEvent();
        statusEvent.setStatus("online");
        zelloChannel.onServerCommand(statusEvent);
        // Verify the state is correct before proceeding
        assertEquals(zelloChannel.getState(), ConnectionState.CONNECTED);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        // ACT
        zelloChannel.sendTextMessage("world");

        // ASSERT
        // We expect send() to be called twice: once for logon, once for the text message.
        // We only care about the last one for this test.
        verify(mockWebSocketClient, atLeastOnce()).send(captor.capture());
        JSONObject json = new JSONObject(captor.getValue()); // Get the last captured value
        assertEquals(json.getString("command"), "send_text_message");
        assertEquals(json.getString("text"), "world");
        assertEquals(json.getString("channel"), "testchannel");
    }

}