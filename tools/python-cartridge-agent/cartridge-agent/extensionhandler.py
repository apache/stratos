#!/usr/bin/env python
import stomp
import time
import logging
import sys
import random
import os
import threading
import socket
import json
from subprocess import Popen,PIPE


def onArtifactUpdatedEvent(extenstionpath,scriptname):
    Process=Popen([os.path.join(extenstionpath,scriptname),str('php')],shell=True,stdin=PIPE,stderr=PIPE)
    print Process.communicate() #now you should see your output
   # os.system()

def onInstanceCleanupMemberEvent(extenstionpath,scriptname):
    Process=Popen([os.path.join(extenstionpath,scriptname),str('php')],shell=True,stdin=PIPE,stderr=PIPE)
    print Process.communicate()

def onInstanceCleanupClusterEvent(extenstionpath,scriptname):
    Process=Popen([os.path.join(extenstionpath,scriptname),str('php')],shell=True,stdin=PIPE,stderr=PIPE)
    print Process.communicate()

def startServerExtension():
    print('=================================startServerExtension')

def onInstanceStartedEvent(extensionpath, scriptname, multitenant, copyartifactscriptname, apppath, envparams):
    print('=================================onInstanceStartedEvent')
    #if ismultitenant
    #   execute copy artifact extension

    if multitenant == "true":
        Process = Popen([os.path.join(extensionpath, copyartifactscriptname), apppath + "/repository/deployment/server/", "/tmp/-1234/"], shell=True, stdin=PIPE, stderr=PIPE)
        print Process.communicate();

    #addPayloadParams()
    Process = Popen([os.path.join(extensionpath, scriptname), envparams], shell=True, stdin=PIPE, stderr=PIPE)
    print Process.communicate();

    #execute instance started extension with empty map

