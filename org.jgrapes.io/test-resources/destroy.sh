#!bin/sh

run=1
trap "echo 1>&2 TERMinated; run=0" TERM 

echo "Started"

while [ $run -eq 1 ]; do 
  sleep 1
done
