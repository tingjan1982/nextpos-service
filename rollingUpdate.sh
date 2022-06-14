#!/bin/zsh

kubectl set image deployment/nextpos-service nextpos-service=docker.io/joelin/nextpos-service:latest
