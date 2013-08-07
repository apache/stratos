#!/usr/bin/env groovy

def response = "curl -k -X POST https://localhost:9445/repo_notification -d payload={'repository':{'url':'https://s2_hostname:8443/git/${repository}'}}".execute().text
println response
