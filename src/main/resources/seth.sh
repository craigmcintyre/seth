#!/bin/bash

# Get the relative path to this file.
DIR=$(dirname "$0")
CLASSPATH=".:*:${DIR}:${DIR}/*"

# Connection details for RapidsSE wireline protocol
HOST="localhost"
PORT="9123"

# Additional arguments for SETH. Enable as required.
#SETH_ARGS="--clean --logtests"   # log all test executions to a file
SETH_ARGS="--clean --logsteps"   # log executions of all steps within each test to a file.

# The SE JDBC URL that tells the JDBC driver how to connect to SE.
JDBC_URL="jdbc:se://${HOST}:${PORT}"

# Run the SE Test Harness
java -cp "${CLASSPATH}" com.rapidsdata.seth.Seth -u "${JDBC_URL}" ${SETH_ARGS}  $@

