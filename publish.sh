#!/usr/bin/env bash

set -ex

sbt "project utils" +publishSigned
sbt sonatypeReleaseAll

echo "Released"