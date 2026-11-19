#!/bin/bash
# usage: final.sh SHOT RES SPP A B THREADS
cd /tmp/claude-0/-home-user-tunas/f7dcaaf2-c182-5d6b-babe-3816fcd936f7/scratchpad/blender
PY=/tmp/claude-0/-home-user-tunas/f7dcaaf2-c182-5d6b-babe-3816fcd936f7/scratchpad/probe/venv/bin/python
$PY scene.py -- --shot $1 --res $2 --spp $3 --frames $4-$5 --threads $6 --out raw/$1 > logs/$1_$4.log 2>&1
echo "EXIT $?" >> logs/$1_$4.log
