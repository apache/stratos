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
import extensionhandler
    

def validateRequiredSystemProperties():
    
    if sys.argv[1] is None:
        print('System property param path not found')
        return
    if sys.argv[2] is None:
        print('System property extension dir path not found')
        return

