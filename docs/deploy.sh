#!/bin/bash

set -e

npm install
npm run docs:build

DEPLOY_FOLDER="deploy/"

if [ ! -d "$DEPLOY_FOLDER" ]; then
  git clone -b gh-pages git@github.com:indix/gocd-s3-artifacts $DEPLOY_FOLDER
fi

cd $DEPLOY_FOLDER
git pull origin gh-pages
rm -rf *
cp -r ../_book/* .

git add -A
git commit -am "Site generated at $(date)"
# git push origin gh-pages