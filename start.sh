#!/bin/bash

echo ""
echo "Starting Keycloak, Coffee Service, Gravitee APIM containers"
echo "-----------------------------------------------------------"
docker compose up -d

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a

echo ""
