# Zabbix Spring Boot Starter - Demo

Demonstrates how to use the Zabbix Spring Boot Starter to expose application metrics to a Zabbix server.

## Features

- **Passive agent**: Zabbix server queries your app for metric values (port 10050)
- **Active agent**: Your app pushes metrics to Zabbix server on a schedule
- **Sender**: Programmatic push of trapper items to Zabbix
- **Custom MetricsProviders**: Register any Spring bean implementing `MetricsProvider`

## Quick Start

```bash
mvn spring-boot:run
```

The agent starts in passive mode on port `10050` by default. Zabbix server can query metrics like:

```
jvm.memory.free
jvm.os.name
app.requests.total
app.uptime
```

## Configuration

See `src/main/resources/application.yml` for all available properties:

| Property | Default | Description |
|---|---|---|
| `zabbix.enabled` | `true` | Enable/disable the starter |
| `zabbix.passive.enabled` | `true` | Enable passive agent (listener) |
| `zabbix.passive.listen-port` | `10050` | Passive agent listen port |
| `zabbix.passive.listen-address` | _(any)_ | Bind to specific interface |
| `zabbix.active.enabled` | `false` | Enable active agent |
| `zabbix.active.host-name` | | Hostname registered in Zabbix |
| `zabbix.active.server-address` | | Zabbix server address |
| `zabbix.active.server-port` | `10051` | Zabbix server port |
| `zabbix.active.refresh-interval` | `120` | Active check refresh interval (seconds) |
| `zabbix.active.psk-identity` | | PSK identity for TLS |
| `zabbix.active.psk` | | PSK hex string for TLS |
| `zabbix.sender.host` | | Sender target host (enables sender bean) |
| `zabbix.sender.port` | `10051` | Sender target port |
| `zabbix.sender.connect-timeout` | `3000` | Connection timeout (ms) |
| `zabbix.sender.socket-timeout` | `3000` | Socket timeout (ms) |

## Custom Metrics Provider

Create a Spring bean implementing `MetricsProvider` and name it with the provider prefix:

```java
@Component("app")
public class AppMetricsProvider implements MetricsProvider {
    @Override
    public Object getValue(MetricsKey mKey) throws MetricsException {
        return switch (mKey.getKey()) {
            case "requests.total" -> totalRequests.get();
            case "uptime" -> ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
            default -> throw new MetricsException("Unknown key: " + mKey.getKey());
        };
    }
}
```

The bean name (`"app"`) becomes the provider prefix. Zabbix queries `app.requests.total` and the starter routes it to this provider with key `requests.total`.

## Using the Sender

Inject `ZabbixSender` to push trapper items programmatically:

```java
@Autowired
ZabbixSender sender;

void sendMetric() throws IOException {
    DataObject data = DataObject.builder()
        .host("my-spring-app")
        .key("app.custom.metric")
        .value("42")
        .build();
    Result result = sender.send(data);
    log.info("Sent: processed={}, failed={}", result.getProcessed(), result.getFailed());
}
```

## Testing with zabbix_get

```bash
zabbix_get -s 127.0.0.1 -p 10050 -k "jvm.memory.free"
zabbix_get -s 127.0.0.1 -p 10050 -k "app.uptime"
```
