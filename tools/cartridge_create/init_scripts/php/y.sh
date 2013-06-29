#!/bin/bash
LOG=/tmp/me.log
CARTRIDGE_AGENT_EPR="http://localhost/me.html"
echo "Sending register request to Cartridge agent service\n" > $LOG
for i in {1..1}
do
    ret=`curl --write-out "%{http_code}\n" --silent --output /dev/null $CARTRIDGE_AGENT_EPR`
    echo "return value:$ret" > $LOG
    #if [[ $ret -eq 2  ]]; then
    #    echo "[curl] Failed to initialize" >> $LOG
    #fi
done
