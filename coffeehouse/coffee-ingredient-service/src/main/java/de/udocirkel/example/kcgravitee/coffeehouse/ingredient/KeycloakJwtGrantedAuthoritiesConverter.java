package de.udocirkel.example.kcgravitee.coffeehouse.ingredient;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@RequiredArgsConstructor
public class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String AUTHORITY_PREFIX = "ROLE_";
    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    private final List<String> audiences;

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return Stream.of(
                        defaultConverter.convert(jwt).stream(),
                        getRealmRoles(jwt).map(this::toAuthority),
                        getResourceRoles(jwt, audiences).map(this::toAuthority)
                )
                .flatMap(Function.identity())
                .collect(Collectors.toSet());
    }

    private Stream<?> getRealmRoles(Jwt jwt) {
        if (!(jwt.getClaim(CLAIM_REALM_ACCESS) instanceof Map<?, ?> realmAccess)) {
            return Stream.empty();
        }
        if (!(realmAccess.get(ROLES) instanceof Collection<?> roles)) {
            return Stream.empty();
        }
        return roles.stream().filter(Objects::nonNull);
    }

    private Stream<?> getResourceRoles(Jwt jwt, List<String> audiences) {
        if (audiences == null || audiences.isEmpty()) {
            return Stream.empty();
        }
        return audiences.stream()
                .flatMap(audience -> getResourceRoles(jwt, audience));
    }

    private Stream<?> getResourceRoles(Jwt jwt, String audience) {
        if (audience == null) {
            return Stream.empty();
        }
        if (!(jwt.getClaim(CLAIM_RESOURCE_ACCESS) instanceof Map<?, ?> resourceAccess)) {
            return Stream.empty();
        }
        if (!(resourceAccess.get(audience) instanceof Map<?, ?> boundResourceAccess)) {
            return Stream.empty();
        }
        if (!(boundResourceAccess.get(ROLES) instanceof Collection<?> roles)) {
            return Stream.empty();
        }
        return roles.stream().filter(Objects::nonNull);
    }

    private GrantedAuthority toAuthority(Object role) {
        return new SimpleGrantedAuthority(AUTHORITY_PREFIX + Objects.toString(role));
    }

}
