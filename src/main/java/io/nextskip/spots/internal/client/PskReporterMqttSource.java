package io.nextskip.spots.internal.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.common.api.SubscriptionStatusProvider;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * PSKReporter MQTT data source using Eclipse Paho MQTT v5 client.
 *
 * <p>Connects to the PSKReporter MQTT broker and subscribes to spot topics.
 * The default topic pattern {@code pskr/filter/v2/+/FT8/#} receives all
 * FT8 spots across all bands.
 *
 * <p>Topic structure:
 * <pre>
 * pskr/filter/v2/{band}/{mode}/{senderCountry}/{senderDxcc}/{receiverCountry}/{receiverDxcc}
 * </pre>
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code pskr/filter/v2/+/FT8/#} - All FT8 spots</li>
 *   <li>{@code pskr/filter/v2/20m/+/#} - All modes on 20m</li>
 *   <li>{@code pskr/filter/v2/+/+/+/+/+/291} - All spots received in USA (DXCC 291)</li>
 * </ul>
 *
 * @see <a href="https://mqtt.pskreporter.info/">PSKReporter MQTT Documentation</a>
 */
@Component
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected List is immutable from @Value")
public class PskReporterMqttSource extends AbstractSpotSource
        implements MqttCallback, SubscriptionStatusProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PskReporterMqttSource.class);
    private static final String SUBSCRIPTION_ID = "pskreporter-mqtt";
    private static final String DISPLAY_NAME = "PSKReporter MQTT";

    private final String brokerUrl;
    private final List<String> topics;
    private final String clientId;

    private MqttClient client;

    public PskReporterMqttSource(
            @Value("${nextskip.spots.mqtt.broker:tcp://mqtt.pskreporter.info:1883}") String brokerUrl,
            @Value("${nextskip.spots.mqtt.topics:pskr/filter/v2/+/FT8/#}") List<String> topics) {
        super();
        this.brokerUrl = brokerUrl;
        this.topics = topics;
        this.clientId = "nextskip-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public String getSourceName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getSubscriptionId() {
        return SUBSCRIPTION_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected void doConnect() throws Exception {
        LOG.info("Connecting to MQTT broker: {}", brokerUrl);
        LOG.info("Subscribing to topics: {}", topics);

        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        client.setCallback(this);

        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setAutomaticReconnectDelay(1, 300);  // 1 sec min, 5 min (300 sec) max
        options.setCleanStart(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);

        client.connect(options);

        // Subscribe to all configured topics
        for (String topic : topics) {
            client.subscribe(topic, 0);
            LOG.info("Subscribed to topic: {}", topic);
        }
    }

    @Override
    protected void doDisconnect() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                client.close();
            } catch (MqttException e) {
                LOG.warn("Error disconnecting MQTT client: {}", e.getMessage());
            }
        }
        client = null;
    }

    @Override
    protected boolean isConnectedInternal() {
        return client != null && client.isConnected();
    }

    /**
     * Package-private setter for testing purposes.
     */
    void setClient(MqttClient client) {
        this.client = client;
    }

    // MqttCallback implementations

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        LOG.warn("MQTT disconnected: {}", disconnectResponse.getReasonString());
        // Paho handles auto-reconnect internally - do NOT call onConnectionLost()
        // which would trigger a competing manual reconnect and cause RC:142 cascade
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        LOG.error("MQTT error: {}", exception.getMessage());
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Graceful handling of any message error
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            emitMessage(payload);
        } catch (RuntimeException e) {
            LOG.debug("Error processing MQTT message: {}", e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        // Not used - we only subscribe, not publish
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            LOG.info("MQTT reconnected to {}", serverURI);
        } else {
            LOG.info("MQTT connected to {}", serverURI);
        }
        // Subscribe (or re-subscribe after reconnection)
        try {
            for (String topic : topics) {
                client.subscribe(topic, 0);
                LOG.info("{} to topic: {}", reconnect ? "Re-subscribed" : "Subscribed", topic);
            }
        } catch (MqttException e) {
            LOG.error("Failed to subscribe after connect: {}. Disconnecting to trigger retry.",
                    e.getMessage());
            try {
                client.disconnect();  // Paho will auto-reconnect
            } catch (MqttException ignored) {
                // Best effort disconnect
            }
        }
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        // Not used - PSKReporter doesn't require authentication
    }
}
