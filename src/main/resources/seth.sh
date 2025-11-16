#!/bin/bash

# Get the relative path to this file.
DIR=$(dirname "$0")
CLASSPATH=".:*:${DIR}:${DIR}/*"

# Database connection details
HOST="localhost"
PORT="9123"
DATABASE="myDatabase"
#USER?
#PASSWORD?

# Additional arguments for SETH. Enable as required.
#SETH_ARGS="--clean --logtests"   # log all test executions to a file
SETH_ARGS="--clean --logsteps"   # log executions of all steps within each test to a file.

# The JDBC URL that identifies the database and how to connect to it
JDBC_URL="jdbc:postgresql://${HOST}:${PORT}/${DATABASE}"

# Run SETH
java -cp "${CLASSPATH}" com.rapidsdata.seth.Seth -u "${JDBC_URL}" ${SETH_ARGS}  $@

