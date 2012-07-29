#!/bin/bash
while [ true ]; do
  run/web_debug.sh "$*"
  sleep 1s;
done;
