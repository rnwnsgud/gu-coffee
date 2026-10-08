#!/usr/bin/env bash

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo -e "${CYAN}======================================================${NC}"
echo -e "${CYAN}   ☕ GU-COFFEE 로컬 인프라 종료 스크립트            ${NC}"
echo -e "${CYAN}======================================================${NC}"

# 1. 인프라 컨테이너 종료
if docker info > /dev/null 2>&1; then
    if [[ "$1" == "--clean" ]]; then
        echo -e "${YELLOW}컨테이너 및 볼륨(DB 데이터, Redis 데이터)을 전량 초기화합니다...${NC}"
        docker compose down -v
        echo -e "${GREEN}✓ 인프라 컨테이너 및 데이터 볼륨이 초기화되었습니다.${NC}"
    else
        echo -e "${YELLOW}컨테이너를 중지합니다 (데이터는 볼륨에 보존됩니다)...${NC}"
        docker compose down
        echo -e "${GREEN}✓ 인프라 컨테이너가 정상적으로 종료되었습니다.${NC}"
    fi
else
    echo -e "${YELLOW}Docker 데몬이 실행 중이지 않아 컨테이너 종료 단계를 건너뜁니다.${NC}"
fi

# 2. 로컬 Spring Boot 애플리케이션 (8080 포트) 프로세스 감지 및 종료
echo -e "\n${YELLOW}로컬 애플리케이션(포트 8080) 구동 여부 확인 중...${NC}"
APP_PIDS=$(lsof -ti :8080 -sTCP:LISTEN 2>/dev/null || true)
if [ -n "$APP_PIDS" ]; then
    echo -e "${YELLOW}8080 포트를 점유 중인 애플리케이션 프로세스(PID: $APP_PIDS)를 종료합니다...${NC}"
    kill $APP_PIDS 2>/dev/null || true
    sleep 1
    STILL_RUNNING=$(lsof -ti :8080 -sTCP:LISTEN 2>/dev/null || true)
    if [ -n "$STILL_RUNNING" ]; then
        kill -9 $STILL_RUNNING 2>/dev/null || true
    fi
    echo -e "${GREEN}✓ 8080 포트 프로세스가 안전하게 종료되었습니다.${NC}"
else
    echo -e "${GREEN}✓ 8080 포트를 점유 중인 프로세스가 없습니다.${NC}"
fi

if [[ "$1" != "--clean" ]]; then
    echo -e "\n${CYAN}DB 데이터까지 완전히 초기화하려면: ./local-stop.sh --clean${NC}"
fi
