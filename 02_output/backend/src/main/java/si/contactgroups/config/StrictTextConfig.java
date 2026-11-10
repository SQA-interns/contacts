package si.contactgroups.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * A text field of a request body must be JSON text (R-003-02, BR-06): a number or a truth value for
 * the phone, the name or any other text is refused as malformed (400) instead of being turned into
 * text, which would store something the User never typed.
 */
@Configuration
public class StrictTextConfig {

  @Bean
  JsonMapperBuilderCustomizer textIsOnlyText() {
    return builder ->
        builder.withCoercionConfig(
            LogicalType.Textual,
            config ->
                config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
  }
}
