.PHONY: setup pc fmt

setup:
	bash scripts/setup-precommit.sh

pc:
	pre-commit run --all-files

fmt:
	./mvnw -q -DskipTests spotless:apply
