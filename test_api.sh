#!/bin/bash
echo "Testing API server health on localhost:8080"
nc -z localhost 8080
if [ $? -eq 0 ]; then
    echo "Server is up and running on port 8080"
else
    echo "Server is not available on port 8080"
fi