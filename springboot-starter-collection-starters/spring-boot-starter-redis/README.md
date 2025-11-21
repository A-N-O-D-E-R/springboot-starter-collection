# Redis Spring Boot Starter

Spring Boot starter for Redis with AWS ElastiCache/Valkey IAM authentication, Kryo serialization, and two-level caching support.

## Features

- **AWS ElastiCache/Valkey Support**: IAM authentication for AWS Redis clusters
- **Kryo Serialization**: Efficient binary serialization with optional Snappy compression
- **Two-Level Caching**: Combine local (in-memory) and distributed (Redis) caching
- **Environment-based Configuration**: Configure via `SHARED_VALKEY_URL` and `SHARED_VALKEY_GROUP`
- **Lettuce Client**: Modern async Redis client with connection pooling
- **Spring Cache Integration**: Full support for Spring's caching abstraction

## Usage

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>spring-boot-starter-redis</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Configuration

### Option 1: Environment Variables

```bash
export SHARED_VALKEY_URL="redis://my-cluster.cache.amazonaws.com:6379"
export SHARED_VALKEY_GROUP="my-application"
```

The starter automatically configures:
- `spring.data.redis.url` from `SHARED_VALKEY_URL`
- `spring.data.redis.client-name` from `SHARED_VALKEY_GROUP`

### Option 2: Application Properties

```properties
spring.data.redis.host=my-cluster.cache.amazonaws.com
spring.data.redis.port=6379
spring.data.redis.client-name=my-application
spring.data.redis.password=your-password
```

### AWS ElastiCache with IAM Authentication

For AWS ElastiCache with IAM authentication:

```properties
spring.data.redis.host=my-cluster.cache.amazonaws.com
spring.data.redis.port=6379
spring.data.redis.username=my-iam-user
spring.data.redis.client-name=my-application
spring.data.redis.ssl.enabled=true
```

The starter automatically configures IAM token authentication using AWS credentials from the default credential chain.

## Basic Usage

### Spring Cache Annotations

```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Cacheable("users")
    public User getUserById(Long id) {
        // This will be cached in Redis
        return userRepository.findById(id).orElse(null);
    }

    @CacheEvict("users")
    public void updateUser(User user) {
        userRepository.save(user);
    }
}
```

Enable caching in your application:

```java
@SpringBootApplication
@EnableCaching
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### RedisTemplate

```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
```

## Advanced Features

### Kryo Serialization

The starter includes a high-performance Kryo serializer with optional compression:

```java
import com.anode.redis.serializer.KryoRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use Kryo serialization with compression
        KryoRedisSerializer<Object> serializer = new KryoRedisSerializer<>(
            true,  // Enable Snappy compression
            kryo -> {
                // Optional: Register classes for better performance
                kryo.register(MyClass.class);
            }
        );

        template.setDefaultSerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}
```

### Two-Level Caching

Combine local (fast) and distributed (shared) caching:

```java
import com.anode.redis.cache.TwoLevelCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager(
            CacheManager level1CacheManager,
            RedisCacheManager level2CacheManager) {
        return new TwoLevelCacheManager(level1CacheManager, level2CacheManager);
    }

    @Bean
    public CacheManager level1CacheManager() {
        // Local in-memory cache
        return new ConcurrentMapCacheManager();
    }

    @Bean
    public RedisCacheManager level2CacheManager(RedisConnectionFactory connectionFactory) {
        // Distributed Redis cache
        return RedisCacheManager.builder(connectionFactory).build();
    }
}
```

Benefits of two-level caching:
- **Fast reads**: First check local memory
- **Shared state**: Fall back to Redis for cache misses
- **Write-through**: Updates go to both levels
- **Consistency**: Local cache updated from Redis on misses

### Kryo-Aware Error Handling

Handle Kryo serialization errors gracefully:

```java
import com.anode.redis.cache.KryoAwareCacheErrorHandler;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new KryoAwareCacheErrorHandler();
    }
}
```

This error handler automatically evicts corrupted cache entries when Kryo deserialization fails.

## AWS Integration

### IAM Authentication

The starter automatically configures IAM authentication for AWS ElastiCache when:
1. AWS credentials are available (from environment, EC2 instance profile, etc.)
2. AWS region is configured
3. Redis username is provided

```properties
spring.data.redis.username=my-iam-username
spring.data.redis.ssl.enabled=true
```

### IAM Policy Requirements

Your IAM user/role needs the following permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "elasticache:Connect"
      ],
      "Resource": "arn:aws:elasticache:region:account-id:replicationgroup:cluster-name"
    }
  ]
}
```

## Configuration Reference

### Redis Connection

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.data.redis.host` | String | `localhost` | Redis server host |
| `spring.data.redis.port` | int | `6379` | Redis server port |
| `spring.data.redis.url` | String | - | Redis URL (alternative to host/port) |
| `spring.data.redis.username` | String | - | Redis username (for IAM auth) |
| `spring.data.redis.password` | String | - | Redis password |
| `spring.data.redis.client-name` | String | - | Client name for identification |
| `spring.data.redis.database` | int | `0` | Database index |

### SSL/TLS

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.data.redis.ssl.enabled` | boolean | `false` | Enable SSL/TLS |

### Connection Pool (Lettuce)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.data.redis.lettuce.pool.max-active` | int | `8` | Maximum connections |
| `spring.data.redis.lettuce.pool.max-idle` | int | `8` | Maximum idle connections |
| `spring.data.redis.lettuce.pool.min-idle` | int | `0` | Minimum idle connections |
| `spring.data.redis.lettuce.pool.max-wait` | Duration | `-1ms` | Max wait for connection |

## Dependencies Included

- `org.springframework.data:spring-data-redis`
- `io.lettuce:lettuce-core` - Async Redis client
- `com.esotericsoftware:kryo` (5.6.2) - Fast serialization
- `org.xerial.snappy:snappy-java` (1.1.10.7) - Compression
- `io.awspring.cloud:spring-cloud-aws-starter` (3.4.0) - AWS integration

## Troubleshooting

### Connection Issues

Check connectivity to Redis:

```bash
# Test with redis-cli
redis-cli -h my-cluster.cache.amazonaws.com -p 6379 ping

# Or with telnet
telnet my-cluster.cache.amazonaws.com 6379
```

Enable debug logging:

```properties
logging.level.com.anode.redis=DEBUG
logging.level.io.lettuce=DEBUG
logging.level.org.springframework.data.redis=DEBUG
```

### AWS IAM Authentication Errors

Common issues:

1. **Invalid credentials**: Ensure AWS credentials are properly configured
2. **Wrong region**: Verify `AWS_REGION` environment variable
3. **IAM permissions**: Check ElastiCache IAM policy
4. **SSL required**: IAM auth requires `spring.data.redis.ssl.enabled=true`

### Serialization Errors

If you encounter Kryo serialization errors:

1. Register your classes explicitly:
   ```java
   new KryoRedisSerializer<>(true, kryo -> {
       kryo.register(MyClass.class);
       kryo.register(MyOtherClass.class);
   })
   ```

2. Use `KryoAwareCacheErrorHandler` to auto-evict corrupted entries

3. Consider disabling compression for debugging:
   ```java
   new KryoRedisSerializer<>(false, kryo -> {})
   ```

## Examples

### Simple Key-Value Operations

```java
@Service
public class DataService {

    private final RedisTemplate<String, String> redisTemplate;

    public void saveData(String key, String value) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(5));
    }

    public String getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
```

### List Operations

```java
public void addToList(String key, String value) {
    redisTemplate.opsForList().rightPush(key, value);
}

public List<String> getList(String key) {
    return redisTemplate.opsForList().range(key, 0, -1);
}
```

### Hash Operations

```java
public void setHashField(String key, String field, String value) {
    redisTemplate.opsForHash().put(key, field, value);
}

public Object getHashField(String key, String field) {
    return redisTemplate.opsForHash().get(key, field);
}
```

### Pub/Sub Messaging

```java
@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer container(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("my-channel"));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(MessageReceiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}

@Component
public class MessageReceiver {
    public void receiveMessage(String message) {
        System.out.println("Received: " + message);
    }
}
```

## Resources

- [Spring Data Redis Documentation](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Lettuce Documentation](https://lettuce.io/core/release/reference/)
- [AWS ElastiCache for Redis](https://aws.amazon.com/elasticache/redis/)
- [Kryo Serialization](https://github.com/EsotericSoftware/kryo)

## License

This starter follows the same license as the parent project.
