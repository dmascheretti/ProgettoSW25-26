#!/bin/bash
cd /tmp/claude-0/-home-user-tunas/f7dcaaf2-c182-5d6b-babe-3816fcd936f7/scratchpad/blender
echo "queue start $(date +%T)" >> logs/queue.log
while pgrep -f "shot shot3" >/dev/null; do sleep 10; done
echo "shot3 done $(date +%T)" >> logs/queue.log
./final.sh shot4 960x540 16 1 30 2 & ./final.sh shot4 960x540 16 31 60 2 & wait
echo "shot4 done $(date +%T)" >> logs/queue.log
./final.sh shot2 960x540 12 1 38 2 & ./final.sh shot2 960x540 12 39 75 2 & wait
echo "shot2 done $(date +%T)" >> logs/queue.log
./final.sh shot1 960x540 12 1 45 2 & ./final.sh shot1 960x540 12 46 90 2 & wait
echo "shot1 done $(date +%T)" >> logs/queue.log
