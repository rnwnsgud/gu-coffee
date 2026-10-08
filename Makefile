.PHONY: help up down run test clean

help:
	@echo "☕ GU-Coffee 로컬 개발 명령어:"
	@echo "  make up     - MySQL 8.0 & Redis 7.x 컨테이너 백그라운드 기동"
	@echo "  make down   - 인프라 컨테이너 중지"
	@echo "  make run    - 인프라 기동 + 스프링 부트 서버 실행 (profile: local-dev)"
	@echo "  make test   - 전체 테스트 실행"
	@echo "  make clean  - 인프라 컨테이너 및 볼륨 데이터 완전 초기화"

up:
	./local-run.sh --infra-only

down:
	./local-stop.sh

run:
	./local-run.sh

test:
	./gradlew test

clean:
	./local-stop.sh --clean
