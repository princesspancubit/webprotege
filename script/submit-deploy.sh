#!/bin/bash

# This script submits the Google Cloud Build job that deploys infrastructure changes

MY_DIR=$(dirname $0)

cd $MY_DIR/..

# Run the merge build, which executes the deployment
gcloud builds submit --project etsy-kb-ci-prod --config cloudbuild.yaml --timeout=3600