.PHONY: up down dev test bid bench clean help

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

clean: ## 清理构建产物
	./mvnw clean

help: ## 显示帮助
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'
