#!/usr/bin/env bash
set -euo pipefail

PROJECT_NAME="${1:?Informe o namespace/projeto OpenShift}"
APP_NAME="${2:-sca-hazelcast-openshift-mvp}"

oc project "${PROJECT_NAME}"

mvn clean package -DskipTests

docker build -f docker/Dockerfile -t "${APP_NAME}:latest" .

docker tag "${APP_NAME}:latest" \
  "image-registry.openshift-image-registry.svc:5000/${PROJECT_NAME}/${APP_NAME}:latest"

docker push \
  "image-registry.openshift-image-registry.svc:5000/${PROJECT_NAME}/${APP_NAME}:latest"

sed "s|SEU_NAMESPACE|${PROJECT_NAME}|g" openshift/03-deployment.yml \
  | oc apply -f -

oc apply -f openshift/01-service-hazelcast-headless.yml
oc apply -f openshift/02-service-http.yml
oc apply -f openshift/04-route.yml
oc apply -f openshift/05-pod-disruption-budget.yml

oc rollout status deployment/${APP_NAME}