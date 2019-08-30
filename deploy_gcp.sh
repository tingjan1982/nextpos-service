#!/bin/sh

# Login using service account: https://cloud.google.com/sdk/gcloud/reference/auth/activate-service-account

gcloud components install kubectl --quiet
gcloud config set project helpful-range-236813
gcloud auth activate-service-account travis-ci-user@helpful-range-236813.iam.gserviceaccount.com --key-file helpful-range-236813-681ca732dc15.json
gcloud config set compute/zone asia-east1-b

# gcloud container clusters create nextpos-cluster --num-nodes=2 --enable-cloud-logging
# kubectl run nextpos-web --image=docker.io/joelin/nextpos-service:latest --port 8080
# kubectl expose deployment nextpos-web --type=LoadBalancer --port 80 --target-port 8080

## document the environment related steps here for future reference.
## https://dzone.com/articles/configuring-spring-boot-on-kubernetes-with-configm
## https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#define-container-environment-variables-using-configmap-data

# kubectl set env deployment/nextpos-web PROFILE=gcp
# kubectl create configmap application-gcp-props --from-file=application-gcp.properties

## volumes and volumeMounts YAML definition:

#  volumeMounts:
#  - mountPath: /config
#    name: application-props
#    readOnly: true

#  volumes:
#  - name: application-props
#    configMap:
#      name: application-gcp-props
#      defaultMode: 420
#      items:
#      - key: application-gcp.properties
#        path: application-gcp.properties

gcloud container clusters get-credentials nextpos-cluster --zone asia-east1-b --project helpful-range-236813
kubectl set image deployment nextpos-web nextpos-web=docker.io/joelin/nextpos-service:latest
kubectl set env deployment/nextpos-web PROFILE=gcp
kubectl scale deployment nextpos-web --replicas=0
kubectl scale deployment nextpos-web --replicas=1
kubectl get services nextpos-web 
