#!/bin/bash

echo ""
echo "Starting Keycloak, Gravitee APIM, Tools, Coffee Services"
echo "--------------------------------------------------------"
docker compose up -d

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a

echo ""
