FROM quay.io/keycloak/keycloak:26.4.0
WORKDIR /opt/keycloak
COPY maven/example-kcgravitee-keycloak-extension-*.jar /opt/keycloak/providers/
RUN bin/kc.sh build

# kc.sh build ist nicht mehr notwendig, solange man Development Mode oder Runtime Mode nutzt.

# USER 1000 für Runtime
#USER 1000

# Startbefehl
#ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]
