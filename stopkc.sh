#!/bin/bash

echo ""
echo "Stopping Keycloak and Coffee Service containers"
echo "-----------------------------------------------"
docker compose -f docker-compose-kc.yml down

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a

echo ""
