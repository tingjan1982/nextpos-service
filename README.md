# nextpos-service



GCP Region/Zones

https://cloud.google.com/compute/docs/regions-zones/

GCP Deploy docker image via Kubernetes engine

https://cloud.google.com/kubernetes-engine/docs/tutorials/hello-app
https://cloud.google.com/cloud-build/docs/quickstart-docker?hl=en_US&_ga=2.125214274.-919887627.1554621557

GCP Logging

https://cloud.google.com/monitoring/kubernetes-engine/legacy-stackdriver/logging

Docker in Travis CI

https://docs.travis-ci.com/user/docker/


## Portal
                                                    
https://travis-ci.org/tingjan1982/nextpos-service


Administration

Connect to Cloud SQL:

sudo docker run -it --rm --entrypoint=/bin/bash -v `pwd`:/a postgres:alpine

psql "sslmode=verify-ca sslrootcert=server-ca.pem sslcert=client-cert.pem sslkey=client-key.pem hostaddr=<hostname> user=<username> dbname=<db name>"

Connect to MongoDB Atlas

mongo "mongodb+srv://nextpos-mongo-cluster-odlrm.gcp.mongodb.net/test" --username <username>
