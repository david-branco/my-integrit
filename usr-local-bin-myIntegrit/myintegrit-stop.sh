#!/bin/bash

myintegrit=`ps aux | grep myIntegrit-1.0-jar-with-dependencies.jar | awk '{print $2}' | head -1`
sudo kill $myintegrit
