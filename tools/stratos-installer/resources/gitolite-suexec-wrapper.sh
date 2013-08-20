#!/bin/bash

USER=$1

export GIT_PROJECT_ROOT="/home/git/repositories"
export GITOLITE_HTTP_HOME="/home/git"
export GIT_HTTP_BACKEND="/usr/lib/git-core/git-http-backend"


exec /usr/share/gitolite/gl-auth-command $USER
