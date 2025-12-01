package de.udocirkel.example.kcgravitee.coffeehouse.menu;

import java.util.*;

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

    private final String audience;

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        var authorities = new HashSet<>(defaultConverter.convert(jwt));

        // Realm roles
        Map<String, Object> realmAccess = jwt.getClaim(CLAIM_REALM_ACCESS);
        if (realmAccess != null && realmAccess.get(ROLES) instanceof Collection<?> roles) {
            authorities.addAll(roles.stream()
                    .map(Object::toString)
                    .map(role -> new SimpleGrantedAuthority(AUTHORITY_PREFIX + role))
                    .toList());
        }

        // Client (resource) roles
        Map<String, Object> resourceAccess = jwt.getClaim(CLAIM_RESOURCE_ACCESS);
        if (resourceAccess != null) {
            var value = resourceAccess.get(audience);
            if (value instanceof Map<?, ?> map && map.get(ROLES) instanceof Collection<?> clientRoles) {
                authorities.addAll(clientRoles.stream()
                        .map(Object::toString)
                        .map(role -> new SimpleGrantedAuthority(AUTHORITY_PREFIX + role))
                        .toList());
            }
        }

        return authorities;
    }

}
