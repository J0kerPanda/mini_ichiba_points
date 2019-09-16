#!/bin/bash

docker run -d --rm \
	--network=gatling_test \
	--name gatling_test_postgres \
	-e POSTGRES_USER="postgres" \
	-e POSTGRES_PASSWORD="postgres" \
	-e POSTGRES_DB="mini_ichiba_points" \
	postgres

POSTGRES_IP="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' gatling_test_postgres)"
docker run -d --rm \
	--network=gatling_test \
	-e DB_HOST=$POSTGRES_IP \
	-e DB_NAME='mini_ichiba_points' \
	-e DB_USER='postgres' \
	-e DB_PASSWORD='postgres' \
	-e API_KEY='gatling_test' \
	--name gatling_test_mini-ichiba-points \
	mini-ichiba-points:latest

# docker build -t gatling_test_run -f Dockerfile.gatling

MARKET_IP="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' gatling_test_mini-ichiba-points)"
docker run -d --rm \
	--network=gatling_test \
	-e MARKET_IP=$MARKET_IP \
	-e API_KEY='gatling_test' \
	--name gatling_test_run \
	-v $PWD/gatling_test_run:/target/gatling/ \
	gatling_test_run
docker container wait gatling_test_run

TIMESTAMP="$(date +"%Y%m%d-%H%M%S")"
ARCHIVE="gatling_test_run.$TIMESTAMP.tar.gz"
tar -zcvf $ARCHIVE gatling_test_run
rm -r gatling_test_run

docker stop gatling_test_mini-ichiba-points
docker stop gatling_test_postgres
