#!/bin/bash
LOG=/tmp/me.log
CARTRIDGE_AGENT_EPR="http://localhost/me.html"
echo "Sending register request to Cartridge agent service\n" > $LOG
for i in {1..2}
do
    #curl --write-out "%{http_code}\n" --silent --output /dev/null $CARTRIDGE_AGENT_EPR
    curl -X POST -H "Content-Type: text/xml" -d @/tmp/request.xml --silent --output /dev/null "$CARTRIDGE_AGENT_EPR"
    ret=$?
    echo "return value:$ret" > $LOG
    if [[ $ret -eq 2  ]]; then
        echo "[curl] Failed to initialize" >> $LOG
    fi
    if [[ $ret -eq 5 || $ret -eq 6 || $ret -eq 7  ]]; then
        echo "[curl] Resolving host failed" >> $LOG
    fi
    if [[ $ret -eq 28 ]]; then
        echo "[curl] Operation timeout" >> $LOG
    fi
    if [[ $ret -eq 55 || $ret -eq 56 ]]; then
        echo "[curl] Failed sending/receiving network data" >> $LOG
    fi
    if [[ $ret -eq 28 ]]; then
        echo "Operation timeout" >> $LOG
    fi
done
if [[ $ret -gt 0 ]]; then
    echo "Sending cluster join message failed. So shutdown instance and exit" >> $LOG
    exit 0
fi
