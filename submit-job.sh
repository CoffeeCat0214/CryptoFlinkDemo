#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Wait for Kafka first ---
echo 'Waiting 20 seconds for Kafka topic creation...'
sleep 20
# ---------------------------

echo 'Waiting up to 60 seconds for JobManager REST interface at http://jobmanager:8081/overview ...'
counter=0
while ! curl -s --fail --max-time 5 http://jobmanager:8081/overview > /dev/null; do
  counter=$((counter+1))
  if [ $counter -ge 12 ]; then
    echo 'ERROR: JobManager did not become ready after 60 seconds.' >&2
    exit 1
  fi
  echo "Retrying JobManager check ($counter/12)..."
  sleep 5
done

echo 'JobManager is ready. Submitting job...'
# Add a small delay just in case JobManager needs a moment more after REST is up
sleep 5

# Use -d (detached mode) so the command submits and returns.
flink run -d -m jobmanager:8081 /opt/flink/usrlib/flink-crypto-demo.jar
SUBMISSION_RESULT=$?

# Check if SUBMISSION_RESULT is set and is 0
if [ -n "$SUBMISSION_RESULT" ] && [ $SUBMISSION_RESULT -eq 0 ]; then
  echo 'Job submitted successfully.'
else
  echo "ERROR: Flink job submission failed with exit code $SUBMISSION_RESULT." >&2
  # Optionally exit here if submission fails
  # exit $SUBMISSION_RESULT 
fi

echo 'Job submission process finished. Keeping script running to keep container alive.'
# Keep the script running
sleep infinity 