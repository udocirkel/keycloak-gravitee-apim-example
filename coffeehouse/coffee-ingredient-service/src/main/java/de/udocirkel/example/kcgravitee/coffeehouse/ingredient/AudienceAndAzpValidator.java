package de.udocirkel.example.kcgravitee.coffeehouse.ingredient;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class AudienceAndAzpValidator implements OAuth2TokenValidator<Jwt> {

    private final String requiredAudience;
    private final String requiredAzp;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        // prüfe Audience
        if (Objects.nonNull(requiredAudience) && !requiredAudience.isBlank()) {
            List<String> aud = jwt.getAudience();
            if (CollectionUtils.isEmpty(aud) || !aud.contains(requiredAudience)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
            }
        }

        // prüfe Authorized Party
        if (Objects.nonNull(requiredAzp) && !requiredAzp.isBlank()) {
            String azp = jwt.getClaimAsString("azp");
            if (azp == null || !azp.equals(requiredAzp)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid authorized party", null));
            }
        }

        return OAuth2TokenValidatorResult.success();
    }

}
