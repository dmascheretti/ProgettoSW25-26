#!/bin/bash
# usage: final.sh SHOT RES SPP A B THREADS
cd "$(dirname "$0")"
PY=${PY:-python3}  # a python with bpy installed (pip install bpy)
$PY scene.py -- --shot $1 --res $2 --spp $3 --frames $4-$5 --threads $6 --out raw/$1 > logs/$1_$4.log 2>&1
echo "EXIT $?" >> logs/$1_$4.log
