#!/bin/bash

# Start gradlew in background and capture its PID
./gradlew runClient > /tmp/gradlelog.log 2>&1 &
GRADLE_PID=$!

# Start lnav in foreground
lnav run/client/logs/latest.log &
LNAV_PID=$!

# Set trap to clean up if script is interrupted
trap "kill $LNAV_PID 2>/dev/null; exit" INT TERM

# Wait for gradlew to finish
wait $GRADLE_PID
GRADLE_EXIT=$?

# Kill lnav once gradle is done
kill $LNAV_PID 2>/dev/null

# Show the gradle output
echo "Gradle process finished with exit code $GRADLE_EXIT"
cat /tmp/gradlelog.log
