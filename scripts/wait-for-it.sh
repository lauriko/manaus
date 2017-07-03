#!/usr/bin/env bash

set -e

host="$1"
shift

port="$1"
shift

cmd="$@"

until nc -z ${host} ${port}
do
  echo "Service is still unavailable on: ${host}:${port}"
  sleep 1
done

echo "Postgres is up - executing command"
exec $cmd

