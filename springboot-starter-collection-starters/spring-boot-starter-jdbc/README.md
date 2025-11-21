# JDBC Spring Boot Starter

Spring Boot starter for JDBC with AWS Advanced JDBC Wrapper support.

## Features

- **AWS Advanced JDBC Wrapper**: Automatic configuration for AWS RDS with failover support
- **PostgreSQL Support**: PostgreSQL driver included
- **Environment-based Configuration**: Configure datasource via environment variables
- **Utility Functions**: Helpful JDBC utilities for PostgreSQL databases

## Usage

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Configuration

### Option 1: Environment Variable

Set the `JDBC_WRAPPER_SHARED_URL` environment variable:

```bash
export JDBC_WRAPPER_SHARED_URL="jdbc:aws-wrapper:postgresql://my-database.cluster-xyz.us-east-1.rds.amazonaws.com:5432/mydb"
```

The starter will automatically:
- Set `spring.datasource.url` to the provided URL
- Configure `spring.datasource.driver-class-name` to `software.amazon.jdbc.Driver` (if URL starts with `jdbc:aws-wrapper:`)

### Option 2: Application Properties

Configure in your `application.properties`:

```properties
spring.datasource.url=jdbc:aws-wrapper:postgresql://my-database.cluster-xyz.us-east-1.rds.amazonaws.com:5432/mydb
spring.datasource.driver-class-name=software.amazon.jdbc.Driver
spring.datasource.username=myuser
spring.datasource.password=mypassword
```

## AWS Advanced JDBC Wrapper

The AWS Advanced JDBC Wrapper provides:

- **Failover Support**: Automatic failover to replica instances
- **Enhanced Monitoring**: Better observability for RDS connections
- **Connection Pooling**: Optimized connection management

### Example URL Format

```
jdbc:aws-wrapper:postgresql://cluster-endpoint:5432/database?wrapperPlugins=failover,efm
```

Common wrapper plugins:
- `failover`: Automatic failover to replica instances
- `efm`: Enhanced failure monitoring
- `iam`: AWS IAM authentication

## JDBC Utilities

### Drop PostgreSQL Database

**WARNING**: This utility drops all tables in the public schema. Use with extreme caution!

```java
import com.anode.jdbc.JdbcUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void resetDatabase() {
        String result = JdbcUtils.dropPostgresDatabase(jdbcTemplate);
        System.out.println(result);
    }
}
```

This utility:
1. Finds all tables in the `public` schema
2. Drops all foreign key constraints
3. Drops all tables
4. Returns a summary of operations

**⚠️ Use only in development/testing environments!**

## Standard Spring Boot JDBC Usage

Once configured, use standard Spring Boot JDBC features:

### JdbcTemplate

```java
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> findAll() {
        return jdbcTemplate.query(
            "SELECT * FROM users",
            (rs, rowNum) -> new User(
                rs.getLong("id"),
                rs.getString("name")
            )
        );
    }
}
```

### Spring Data JPA

This starter is compatible with Spring Data JPA:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    // getters and setters
}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
```

## Dependencies Included

- `software.amazon.jdbc:aws-advanced-jdbc-wrapper` (2.6.0)
- `org.postgresql:postgresql` (runtime)
- `org.springframework:spring-jdbc`
- `org.apache.commons:commons-lang3`

## Environment Post Processor

This starter uses a Spring Boot `EnvironmentPostProcessor` that runs early in the application lifecycle to configure the datasource from the `JDBC_WRAPPER_SHARED_URL` environment variable.

Priority: `LOWEST_PRECEDENCE` (runs after other processors)

## Troubleshooting

### Connection Issues

If you encounter connection issues:

1. **Verify URL format**: Ensure the JDBC URL is correct
   ```bash
   echo $JDBC_WRAPPER_SHARED_URL
   ```

2. **Check network connectivity**: Ensure you can reach the database host
   ```bash
   telnet my-database.cluster-xyz.us-east-1.rds.amazonaws.com 5432
   ```

3. **Verify credentials**: Check username and password are correct

4. **AWS IAM**: If using IAM authentication, ensure your AWS credentials are properly configured

### Driver Not Found

If you see "No suitable driver found":

1. Ensure the URL starts with `jdbc:aws-wrapper:` if using AWS wrapper
2. Or use standard PostgreSQL URL: `jdbc:postgresql://...`
3. Verify the driver class name is correct in configuration

### Logging

Enable debug logging to see configuration details:

```properties
logging.level.com.anode.jdbc=DEBUG
logging.level.software.amazon.jdbc=DEBUG
```

## Examples

### Basic PostgreSQL Connection

```properties
JDBC_WRAPPER_SHARED_URL=jdbc:postgresql://localhost:5432/mydb
```

### AWS RDS with Failover

```properties
JDBC_WRAPPER_SHARED_URL=jdbc:aws-wrapper:postgresql://my-cluster.cluster-xyz.us-east-1.rds.amazonaws.com:5432/mydb?wrapperPlugins=failover
```

### AWS RDS with IAM Authentication

```properties
JDBC_WRAPPER_SHARED_URL=jdbc:aws-wrapper:postgresql://my-cluster.cluster-xyz.us-east-1.rds.amazonaws.com:5432/mydb?wrapperPlugins=iam,failover
```

## Resources

- [AWS Advanced JDBC Wrapper Documentation](https://github.com/awslabs/aws-advanced-jdbc-wrapper)
- [Spring Boot JDBC Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.datasource)
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/)

## License

This starter follows the same license as the parent project.
