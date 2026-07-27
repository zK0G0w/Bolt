.PHONY: up down dev test bid bench reseed clean help

REDIS_PASSWORD ?= redis123456
# dev profile 使用 db10，见 application-dev.yml
REDIS_DB ?= 10
# 未使用 docker compose 时的 Redis 容器名，如 make reseed REDIS_CONTAINER=redis7
REDIS_CONTAINER ?= redis

.DEFAULT_GOAL := help

up: ## Docker 一键启动（Redis + App）
	docker compose up -d

down: ## 停止并清理容器
	docker compose down

dev: ## 本地开发启动（需本机 Redis）
	./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

test: ## 运行测试
	./mvnw test

bid: ## 发起一次竞价请求
	@curl -s -X POST http://localhost:9292/bid \
		-H "Content-Type: application/json" \
		-d @examples/sample-bid-request.json | python3 -m json.tool

bench: ## 压测（200 并发，2000 请求）
	hey -n 2000 -c 200 -m POST \
		-H "Content-Type: application/json" \
		-D examples/sample-bid-request.json \
		http://localhost:9292/bid

reseed: ## 清空 Redis 中 db$(REDIS_DB) 的 bolt:* 键，重启应用后重新播种
	@EXEC="docker compose exec -T redis"; \
	docker compose ps --services --status running 2>/dev/null | grep -qx redis \
		|| EXEC="docker exec $(REDIS_CONTAINER)"; \
	CLI="redis-cli -a $(REDIS_PASSWORD) --no-auth-warning -n $(REDIS_DB)"; \
	KEYS=$$($$EXEC $$CLI --scan --pattern 'bolt:*' 2>/dev/null) \
		|| { echo "无法连接 Redis 容器，可指定 REDIS_CONTAINER=<名称>"; exit 1; }; \
	if [ -n "$$KEYS" ]; then \
		echo "$$KEYS" | tr '\n' ' ' | xargs $$EXEC $$CLI del; \
		echo "已清空，重启应用后重新播种"; \
	else echo "db$(REDIS_DB) 无 bolt:* 键，无需清理"; fi

clean: ## 清理构建产物
	./mvnw clean

help: ## 显示帮助
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'
