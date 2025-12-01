#!/bin/bash

echo ""
echo "Stopping Keycloak, Coffee Service, Gravitee APIM containers"
echo "-----------------------------------------------------------"
docker compose down

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a

echo ""
