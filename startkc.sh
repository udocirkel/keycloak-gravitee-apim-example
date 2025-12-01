#!/bin/bash

echo ""
echo "Starting Keycloak and Coffee Service containers"
echo "-----------------------------------------------"
docker compose -f docker-compose-kc.yml up -d

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a

echo ""
