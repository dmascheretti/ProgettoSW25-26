#!/bin/bash
# upscale raw 960x540 -> 1920x1080 lanczos + light per-shot grade
cd /tmp/claude-0/-home-user-tunas/f7dcaaf2-c182-5d6b-babe-3816fcd936f7/scratchpad/blender
declare -A G
G[shot1]="eq=saturation=0.78:gamma=0.97,colorbalance=bs=0.04:bm=0.02:rh=-0.02"
G[shot2]="eq=saturation=1.05,colorbalance=rm=0.03:gm=0.01:bm=-0.03:rh=0.03"
G[shot3]="eq=saturation=1.0"
G[shot4]="eq=saturation=1.02"
for s in ${@:-shot1 shot2 shot3 shot4}; do
  rm -rf out/$s; mkdir -p out/$s
  ffmpeg -y -loglevel error -start_number 1 -i raw/$s/%04d.png -vf "scale=1920:1080:flags=lanczos,${G[$s]},unsharp=5:5:0.35" -start_number 1 out/$s/%04d.png
  echo "$s $(ls out/$s | wc -l) frames"
done
