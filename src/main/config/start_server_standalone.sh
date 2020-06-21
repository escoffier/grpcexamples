#!/usr/bin/env bash

./consul agent -Server -bind=192.168.21.241 -data-dir=./data-standalone -client=192.168.21.241 -config-dir=./config -enable-local-script-checks -node=241 -bootstrap
