#!/bin/sh

container_id="$1"

[ $# -eq 0 ] && { echo "Usage: $0 container_id"; exit 1; }

if [ ! -e /usr/local/bin/nsenter ]; then
  docker run --rm -v /usr/local/bin:/target jpetazzo/nsenter
fi

PID=$(docker inspect --format {{.State.Pid}} $container_id)

nsenter --target $PID --mount --uts --ipc --net --pid
