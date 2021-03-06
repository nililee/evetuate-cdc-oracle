#! /bin/bash

export TERM=dumb

set -e

GRADLE_OPTS=""

if [ "$1" = "--clean" ] ; then
  GRADLE_OPTS="clean"
  shift
fi

./gradlew ${GRADLE_OPTS} $* testClasses

. ./scripts/set-env-postgres-polling.sh

docker-compose -f docker-compose-postgres.yml  up --build -d

./scripts/wait-for-postgres.sh

./gradlew $* :eventuate-local-java-cdc-connector-polling:cleanTest :eventuate-local-java-cdc-connector-polling:test -Dtest.single=PollingDaoIntegrationTest

docker-compose -f docker-compose-postgres.yml down -v --remove-orphans
