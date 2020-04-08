#!/bin/sh


echo "*** Startup $0 suceeded now starting service using exec to expand CMD variables ***"
exec java -jar ${JAVA_OPTS} /app.jar server /config.yml
