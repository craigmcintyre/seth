#!/bin/bash

# Get the relative path to this file.
DIR=$(dirname "$0")
CLASSPATH=".:*:${DIR}:${DIR}/*"

# The path to the se-shell
SE_PATH="../OS X/Build/Products/Debug/SE"

# Any other arguments to be provided to the se-shell in addition to
# the --metadata and --script arguments which will automatically be added.
SE_ARGS="--name NODE1 --sen ../Sen --commands ../Commands"  # in addition to --metadata and --script

# Additional arguments for SETH. Enable as required.
#SETH_ARGS="--clean --logtests"   # log all test executions to a file
SETH_ARGS="--clean --logsteps"   # log executions of all steps within each test to a file.

# The SE JDBC URL that tells the JDBC driver how to connect to SE.
JDBC_URL="jdbc:rapidsse:${SE_PATH}?extraargs=${SE_ARGS}"

# Run the SE Test Harness
java -cp ${CLASSPATH} com.rapidsdata.seth.Seth -u "${JDBC_URL}" ${SETH_ARGS}  $@

