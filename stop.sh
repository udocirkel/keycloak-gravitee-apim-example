#!/bin/bash

if [ -z "$(docker ps -a -q)" ]; then
  echo "Keine Container vorhanden."
else
  echo "Container werden gestoppt."
  docker stop $(docker ps -a -q)

  echo "Container werden gelöscht."
  docker rm $(docker ps -a -q)

  echo "Container werden geprüft."
  docker ps -a
fi
