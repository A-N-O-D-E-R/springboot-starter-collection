package com.anode.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Utility class for JDBC operations.
 */
public class JdbcUtils {

    private JdbcUtils() {}

    /**
     * Drops all tables in a PostgreSQL database.
     * This is useful for development/testing purposes.
     *
     * WARNING: This will delete all data! Use with caution.
     *
     * @param jdbcTemplate the JDBC template
     * @return a string describing the operations performed
     */
    public static String dropPostgresDatabase(JdbcTemplate jdbcTemplate) {
        var result = new StringBuilder(1024);
        var tables = jdbcTemplate.query(
                "SELECT concat(table_schema,'.',table_name) AS table_schema_name FROM information_schema.tables where TABLE_SCHEMA='public'",
                (rs, rowNum) -> rs.getString("table_schema_name"));
        tables.forEach(table -> jdbcTemplate.query(
                        "SELECT conname FROM pg_constraint WHERE conrelid = '" + table + "'::regclass AND contype = 'f'",
                        (rs, rowNum) -> "ALTER TABLE " + table + " DROP CONSTRAINT " + rs.getString("conname"))
                .forEach(dropConstraintQuery -> {
                    result.append(dropConstraintQuery).append("\n");
                    jdbcTemplate.execute(dropConstraintQuery);
                }));
        tables.forEach(table -> {
            var dropTableQuery = "DROP TABLE " + table;
            result.append(dropTableQuery).append("\n");
            jdbcTemplate.execute(dropTableQuery);
        });
        result.append("\n\n").append("Database dropped, redeploy needed!");
        return result.toString();
    }
}
