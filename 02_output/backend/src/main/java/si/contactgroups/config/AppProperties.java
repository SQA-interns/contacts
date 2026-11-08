package si.contactgroups.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/** Typed view of the environment variables (ES-01, docs/02_contracts/configuration.schema.json). */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @NotBlank String firebaseProjectId,
    @NotBlank String corsAllowedOrigin,
    @Min(1) int rateLimitPerMinute,
    DataSize maxRequestSize,
    boolean requireHttps,
    @NotBlank String trashPurgeCron) {}
