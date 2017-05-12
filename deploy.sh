#!/bin/bash

sbt "set test in assembly := {}" clean editsource:edit assembly
