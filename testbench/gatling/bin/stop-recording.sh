#!/bin/bash
kill -s SIGTERM $(cat /app/.gatling-recorder-pid)