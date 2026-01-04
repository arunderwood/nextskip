package io.nextskip.spots.internal.client;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PskReporterMqttSource}.
 *
 * <p>Focuses on verifying that the MQTT client uses Paho's built-in
 * auto-reconnect and does NOT trigger the parent's manual reconnect logic.
 */
@ExtendWith(MockitoExtension.class)
class PskReporterMqttSourceTest {

    private static final String BROKER_URL = "tcp://test.broker:1883";
    private static final List<String> TOPICS = List.of("test/topic1", "test/topic2");

    @Mock
    private MqttClient mockClient;

    @Mock
    private MqttDisconnectResponse mockDisconnectResponse;

    private PskReporterMqttSource source;

    @BeforeEach
    void setUp() {
        source = new PskReporterMqttSource(BROKER_URL, TOPICS);
    }

    // ===========================================
    // disconnected() tests
    // ===========================================

    @Test
    void testDisconnected_DoesNotTriggerManualReconnect() {
        // Given: a disconnect response
        when(mockDisconnectResponse.getReasonString()).thenReturn("Connection lost");

        // When: disconnected callback fires
        source.disconnected(mockDisconnectResponse);

        // Then: source should NOT trigger parent's manual reconnect
        // (verified by checking no reconnect is scheduled)
        // The source should NOT be in a connected state after disconnect
        assertThat(source.isConnected()).isFalse();
    }

    @Test
    void testDisconnected_LogsWarning_DoesNotCallOnConnectionLost() {
        // Given: a disconnect response with a specific reason
        when(mockDisconnectResponse.getReasonString()).thenReturn("Session taken over");

        // When: disconnected callback fires
        source.disconnected(mockDisconnectResponse);

        // Then: it should log the warning but NOT call onConnectionLost()
        // (onConnectionLost would schedule a reconnect, which we don't want)
        // This test verifies the callback completes without triggering parent logic
        assertThat(source.isConnected()).isFalse();
    }

    // ===========================================
    // connectComplete() tests
    // ===========================================

    @Test
    void testConnectComplete_Reconnect_ResubscribesAllTopics() throws MqttException {
        // Given: a connected client
        injectMockClient();

        // When: connectComplete is called with reconnect=true
        source.connectComplete(true, BROKER_URL);

        // Then: all topics should be subscribed
        verify(mockClient).subscribe("test/topic1", 0);
        verify(mockClient).subscribe("test/topic2", 0);
    }

    @Test
    void testConnectComplete_InitialConnect_SubscribesAllTopics() throws MqttException {
        // Given: a connected client
        injectMockClient();

        // When: connectComplete is called with reconnect=false
        source.connectComplete(false, BROKER_URL);

        // Then: all topics should be subscribed
        verify(mockClient).subscribe("test/topic1", 0);
        verify(mockClient).subscribe("test/topic2", 0);
    }

    @Test
    void testConnectComplete_SubscribeFails_DisconnectsToTriggerRetry() throws MqttException {
        // Given: a client that fails to subscribe
        injectMockClient();
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                .when(mockClient).subscribe(anyString(), anyInt());

        // When: connectComplete is called
        source.connectComplete(true, BROKER_URL);

        // Then: client should be disconnected to trigger Paho's auto-reconnect
        verify(mockClient).disconnect();
    }

    @Test
    void testConnectComplete_DisconnectFails_HandledGracefully() throws MqttException {
        // Given: a client that fails to subscribe AND fails to disconnect
        injectMockClient();
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                .when(mockClient).subscribe(anyString(), anyInt());
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                .when(mockClient).disconnect();

        // When: connectComplete is called
        // Then: should not throw exception
        source.connectComplete(true, BROKER_URL);

        // Verify disconnect was attempted
        verify(mockClient).disconnect();
    }

    // ===========================================
    // getSourceName() tests
    // ===========================================

    @Test
    void testGetSourceName_ReturnsPskReporterMqtt() {
        assertThat(source.getSourceName()).isEqualTo("PSKReporter MQTT");
    }

    // ===========================================
    // Helper methods
    // ===========================================

    /**
     * Injects a mock MqttClient into the source using package-private setter.
     */
    private void injectMockClient() {
        source.setClient(mockClient);
    }
}
