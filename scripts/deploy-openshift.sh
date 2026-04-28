#!/usr/bin/env bash
set -euo pipefail

PROJECT_NAME="${1:-sca-dev}"
IMAGE_NAME="${2:-sca-service}"

oc project "${PROJECT_NAME}"

mvn clean package -DskipTests

docker build -t "${IMAGE_NAME}:latest" .

# Ajuste conforme seu registry/namespace, caso use build externo.
# docker tag "${IMAGE_NAME}:latest" "image-registry.openshift-image-registry.svc:5000/${PROJECT_NAME}/${IMAGE_NAME}:latest"
# docker push "image-registry.openshift-image-registry.svc:5000/${PROJECT_NAME}/${IMAGE_NAME}:latest"

oc apply -f openshift/01-service-hazelcast-headless.yml
oc apply -f openshift/02-service-http.yml
oc apply -f openshift/03-deployment.yml
oc apply -f openshift/04-route.yml
oc apply -f openshift/05-pod-disruption-budget.yml
