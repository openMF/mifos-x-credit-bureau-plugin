bootrun:
	docker compose up -d
	./gradlew bootrun

restart:
	docker compose down -v
