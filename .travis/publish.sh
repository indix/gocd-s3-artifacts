#!/usr/bin/env bash

if ([ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]);
then
    echo "Triggering a versioned release of the project"
    echo "Attempting to publish signed jar"
    sbt "project utils" +publishSigned
    echo "Published the signed jar"
    echo "Attempting to make a release of the sonatype staging"
    sbt sonatypeReleaseAll
    echo "Released the sonatype staging setup"
else
    echo "Not running publish since we're either not in master or in a Pull Request"
fi