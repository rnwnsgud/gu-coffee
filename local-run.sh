#!/usr/bin/env bash

set -e

# ANSI Color Codes
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo -e "${CYAN}======================================================${NC}"
echo -e "${CYAN}   ☕ GU-COFFEE 로컬 인프라 & 개발 환경 부트스트랩   ${NC}"
echo -e "${CYAN}======================================================${NC}"

# 1. Docker 데몬 구동 여부 확인
echo -e "\n${YELLOW}[1/4] Docker 데몬 상태 확인 중...${NC}"
if ! docker info > /dev/null 2>&1; then
    echo -e "${YELLOW}Docker 데몬이 실행 중이지 않습니다. Docker Desktop 실행을 시도합니다...${NC}"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        open -a Docker || true
        echo -e "Docker Desktop이 켜질 때까지 대기 중..."
        MAX_WAIT=30
        COUNT=0
        while ! docker info > /dev/null 2>&1; do
            sleep 2
            COUNT=$((COUNT + 2))
            if [ $COUNT -ge $MAX_WAIT ]; then
                echo -e "${RED}오류: Docker 데몬 기동 시간이 초과되었습니다. Docker Desktop을 직접 켜주세요.${NC}"
                exit 1
            fi
        done
        echo -e "${GREEN}✓ Docker 데몬이 성공적으로 준비되었습니다.${NC}"
    else
        echo -e "${RED}오류: Docker 데몬이 실행 중이지 않습니다. Docker 서비스를 실행해 주세요.${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Docker 데몬이 정상 작동 중입니다.${NC}"
fi

# 2. Docker Compose 인프라 컨테이너 기동
echo -e "\n${YELLOW}[2/4] MySQL 8.0 & Redis 7.x 컨테이너 기동 중...${NC}"
docker compose up -d

# 3. 인프라 Healthcheck 대기
echo -e "\n${YELLOW}[3/4] 인프라 정상 구동 대기 (Healthcheck 대기 중)...${NC}"

wait_for_health() {
    local service=$1
    local max_wait=40
    local count=0

    echo -n " - $service 상태 확인 중: "
    while true; do
        local status
        status=$(docker compose ps --format '{{.Health}}' "$service" 2>/dev/null || echo "starting")

        if [ "$status" == "healthy" ]; then
            echo -e " ${GREEN}정상 (healthy)${NC}"
            break
        fi

        sleep 2
        count=$((count + 2))
        echo -n "."

        if [ $count -ge $max_wait ]; then
            echo -e "\n${RED}경고: $service 헬스체크 대기 시간 초과 (40초). 계속 진행합니다.${NC}"
            break
        fi
    done
}

wait_for_health "mysql"
wait_for_health "redis"

echo -e "${GREEN}✓ 로컬 인프라 (MySQL: 3306, Redis: 6379) 준비 완료!${NC}"

# --infra-only 플래그가 전달되었는지 확인
if [[ "$1" == "--infra-only" ]]; then
    echo -e "\n${CYAN}======================================================${NC}"
    echo -e "${GREEN} 인프라 컨테이너가 성공적으로 기동되었습니다. (--infra-only)${NC}"
    echo -e " MySQL : localhost:3306 (DB: gu_coffee, User: coffee, Pwd: coffee)"
    echo -e " Redis : localhost:6379"
    echo -e "${CYAN}======================================================${NC}"
    exit 0
fi

# 4. Spring Boot 애플리케이션 실행 (local-dev 프로파일)
echo -e "\n${YELLOW}[4/4] Spring Boot 애플리케이션 기동 (profile: local-dev)...${NC}"
echo -e "${CYAN}Flyway 마이그레이션이 실행되고 coffee-server가 구동됩니다...${NC}\n"

# JAVA_HOME 자동 탐색 (설정되어 있지 않은 경우)
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Users/gujunhyeong/Library/Java/JavaVirtualMachines/ms-21.0.12.1/Contents/Home" ]; then
        export JAVA_HOME="/Users/gujunhyeong/Library/Java/JavaVirtualMachines/ms-21.0.12.1/Contents/Home"
    elif [[ "$OSTYPE" == "darwin"* ]] && command -v /usr/libexec/java_home > /dev/null 2>&1; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home 2>/dev/null)
    fi
fi

# 8080 포트 사전 충돌 확인 및 정리
EXISTING_PID=$(lsof -ti :8080 -sTCP:LISTEN 2>/dev/null || true)
if [ -n "$EXISTING_PID" ]; then
    echo -e "${YELLOW}8080 포트를 점유 중인 기존 프로세스(PID: $EXISTING_PID)를 종료합니다...${NC}"
    kill -9 $EXISTING_PID 2>/dev/null || true
    sleep 1
fi

SPRING_PROFILES_ACTIVE=local-dev ./gradlew :coffee-server:bootRun
