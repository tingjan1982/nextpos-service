#!/bin/sh

# Login using service account: https://cloud.google.com/sdk/gcloud/reference/auth/activate-service-account

gcloud components install kubectl --quiet
gcloud config set project helpful-range-236813
gcloud auth activate-service-account travis-ci-user@helpful-range-236813.iam.gserviceaccount.com --key-file helpful-range-236813-681ca732dc15.json
gcloud config set compute/zone asia-east1-b

#gcloud container clusters create nextpos-cluster --num-nodes=2 --enable-cloud-logging
#kubectl run nextpos-web --image=docker.io/joelin/nextpos-service:latest --port 8080
#kubectl expose deployment nextpos-web --type=LoadBalancer --port 80 --target-port 8080

gcloud container clusters get-credentials nextpos-cluster --zone asia-east1-b --project helpful-range-236813
kubectl set image deployment nextpos-web nextpos-web=docker.io/joelin/nextpos-service:latest
kubectl scale deployment nextpos-web --replicas=0
kubectl scale deployment nextpos-web --replicas=1
kubectl get services nextpos-web 
