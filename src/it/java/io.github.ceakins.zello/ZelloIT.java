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

        // When the factory is asked to create a client, we return our mock
        when(mockWebSocketFactory.create(any(), any(), any())).thenReturn(mockWebSocketClient);

        // Inject our mocks into the ZelloChannel instance
        zelloChannel = new ZelloChannel(config, mockAudioEngine, mockWebSocketFactory);
        zelloChannel.setListener(mockListener);

        // We need to call connect() to initialize the internal webSocketClient field
        zelloChannel.connect();
    }

    @Test
    public void testConnect_StartsWebSocket() {
        // The connect() call is in setup(). We just verify it worked.
        verify(mockWebSocketClient, times(1)).connect();
        assertEquals(zelloChannel.getState(), ConnectionState.CONNECTING);
    }

    @Test
    public void testOnOpen_SendsLogonCommand() {
        // Arrange: Capture the string sent to the websocket
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        // Act: Simulate the websocket connection opening
        zelloChannel.onOpen();

        // Assert: Verify that send was called and capture the argument
        verify(mockWebSocketClient, times(1)).send(captor.capture());
        String sentJson = captor.getValue();
        JSONObject json = new JSONObject(sentJson);

        assertEquals(zelloChannel.getState(), ConnectionState.LOGGING_IN);
        assertEquals(json.getString("command"), "logon");
        assertEquals(json.getString("username"), "testuser");
        assertEquals(json.getString("channel"), "testchannel");
    }

    @Test
    public void testOnServerCommand_ChannelStatusOnline_NotifiesListener() {
        // Arrange
        OnChannelStatusEvent event = new OnChannelStatusEvent();
        event.setStatus("online");

        // Act: Simulate receiving the event from the server
        zelloChannel.onServerCommand(event);

        // Assert
        assertEquals(zelloChannel.getState(), ConnectionState.CONNECTED);
        verify(mockListener, times(1)).onConnected();
    }

    @Test
    public void testOnServerCommand_TextMessage_NotifiesListener() {
        // Arrange
        OnTextMessageEvent event = new OnTextMessageEvent();
        event.setFrom("otheruser");
        event.setMessage("Hello");

        // Act
        zelloChannel.onServerCommand(event);

        // Assert
        verify(mockListener, times(1)).onTextMessage("otheruser", "Hello");
    }

    @Test
    public void testSendTextMessage_SendsCorrectJson() {
        // Arrange: first, we need to put the channel in the 'CONNECTED' state
        zelloChannel.onServerCommand(new OnChannelStatusEvent()); // A blank one is fine for this

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        // Act
        zelloChannel.sendTextMessage("world");

        // Assert
        verify(mockWebSocketClient, times(1)).send(captor.capture());
        JSONObject json = new JSONObject(captor.getValue());
        assertEquals(json.getString("command"), "send_text_message");
        assertEquals(json.getString("text"), "world");
        assertEquals(json.getString("channel"), "testchannel");
    }

}
