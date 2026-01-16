#!/bin/bash

echo ""
echo "Stopping Keycloak, Gravitee APIM, Tools, Coffee Services"
echo "--------------------------------------------------------"
docker compose down

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a

echo ""
