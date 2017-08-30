#!/bin/bash

sbt "set test in assembly := {}" clean assembly
