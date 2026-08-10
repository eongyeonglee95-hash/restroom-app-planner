#!/bin/sh
set -e

# OSRM을 먼저 백그라운드로 띄운다. mmap 기반이라 기동이 빠르고(로컬 실측 0.3초 안팎),
# 아직 준비되기 전에 첫 요청이 오더라도 WalkingRouteService가 Tmap -> 카카오 -> 직선거리
# 순으로 자동 폴백하므로 두 프로세스의 기동 순서를 엄격히 맞출 필요는 없다.
osrm-routed --algorithm mld --port 5001 /app/osrm-data/seoul.osrm &
OSRM_PID=$!

# 컨테이너가 내려갈 때 OSRM도 같이 정리한다(exec로 PID 1이 java로 바뀌므로, 없으면
# 백그라운드 프로세스가 신호를 못 받을 수 있다).
trap 'kill "$OSRM_PID" 2>/dev/null' TERM INT

# Cloud Run은 $PORT 환경변수로 실제 리스닝 포트를 지정한다. application.properties의
# server.port=${PORT:8080}이 이를 그대로 받는다.
exec java -jar /app/app.jar
