#!/bin/bash

set -ex
cd "$(dirname "$0")"
yarn install
yarn run docs:build