package io.nextskip.spots.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for PSKReporter MQTT connection.
 *
 * <p>Uses {@code @ConfigurationProperties} instead of {@code @Value} because
 * Spring's {@code @Value} annotation does not properly bind YAML lists to
 * {@code List<String>} — it falls back to the default value. This is
 * especially problematic with MQTT topic patterns containing {@code #}
 * (wildcard) characters.
 *
 * <p>Configured via {@code nextskip.spots.mqtt} in application.yml:
 * <pre>
 * nextskip:
 *   spots:
 *     mqtt:
 *       broker: tcp://mqtt.pskreporter.info:1883
 *       topics:
 *         - pskr/filter/v2/+/FT8/#
 *         - pskr/filter/v2/+/FT4/#
 *         - pskr/filter/v2/+/FT2/#
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "nextskip.spots.mqtt")
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Spring ConfigurationProperties requires mutable getter/setter for binding")
public class MqttProperties {

    private String broker = "tcp://mqtt.pskreporter.info:1883";
    private List<String> topics = new ArrayList<>(List.of("pskr/filter/v2/+/FT8/#"));

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }
}
