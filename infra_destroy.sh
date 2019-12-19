#!/bin/bash
touch infrastructure/terraform.tfstate;
touch infrastructure/terraform.tfstate.backup;
touch infrastructure/manifests/out/deployment.yaml;
touch infrastructure/manifests/out/service.yaml;
docker run \
    -v ${PWD}/infrastructure/account.json:/usr/infrastructure/account.json \
    -v ${PWD}/infrastructure/terraform.tfstate:/usr/infrastructure/terraform.tfstate \
    -v ${PWD}/infrastructure/terraform.tfstate.backup:/usr/infrastructure/terraform.tfstate.backup \
    -v ${PWD}/infrastructure/manifests/out/deployment.yaml:/usr/infrastructure/manifests/out/deployment.yaml \
    -v ${PWD}/infrastructure/manifests/out/service.yaml:/usr/infrastructure/manifests/out/service.yaml \
    imdb_infra destroy -auto-approve
