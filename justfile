set shell := ["sh", "-c"]
set allow-duplicate-recipes
set positional-arguments
set dotenv-path := ".local/.env"
# set dotenv-load := true
set export

postgres_sql_file := justfile_directory() + "/docker/postgres/docker-entrypoint-initdb.d/postgres.sql"
postgres_data_dir := justfile_directory() + "/docker/data/.postgres"

alias dc-u := docker-compose-up
alias dc-d := docker-compose-down
alias clean-dd := clean-docker-compose-data

api-setup:
  sbt generateSmithyFromOpenApi

docker-compose-up-pre:
  #!/usr/bin/env bash
  set -euxo pipefail

  if [[ ! -d "{{postgres_data_dir}}" ]]; then
    cat docker/postgres/ddls/create_user.sql \
      docker/postgres/ddls/create_db.sql \
      docker/postgres/ddls/domains/*.sql \
      docker/postgres/ddls/akka.sql \
      docker/postgres/ddls/grant_permissions.sql \
      docker/postgres/ddls/create_test_db.sql \
      docker/postgres/ddls/domains/*.sql \
      docker/postgres/ddls/akka.sql \
      docker/postgres/ddls/grant_permissions.sql > {{postgres_sql_file}}
  fi

docker-compose-down-pre:
  #!/usr/bin/env bash
  set -euxo pipefail

  if [[ -f "{{postgres_sql_file}}" ]]; then
    rm {{postgres_sql_file}}
  fi

docker-compose-up: docker-compose-up-pre
  #!/usr/bin/env bash
  set -euxo pipefail

  if [ -f  docker/.profile ]; then
    profile=$(cat docker/.profile)
    if [ "$profile" == "microk8s" ]; then
      if [[ {{os()}} == "macos" ]]; then
        docker compose --profile microk8s --profile macos -f docker/docker-compose.yml down
      else
        docker compose --profile microk8s --profile linux -f docker/docker-compose.yml down
      fi
      echo local > docker/.profile
      if [[ {{os()}} == "macos" ]]; then
        docker compose --profile local --profile macos -f docker/docker-compose.yml up -d
      else
        docker compose --profile local --profile linux -f docker/docker-compose.yml up -d
      fi
    fi
  else
    echo local > docker/.profile
    if [[ {{os()}} == "macos" ]]; then
      docker compose --profile local --profile macos -f docker/docker-compose.yml up -d
    else
      docker compose --profile local --profile linux -f docker/docker-compose.yml up -d
    fi
  fi

docker-compose-down: docker-compose-down-pre
  #!/usr/bin/env bash
  set -euxo pipefail

  profile=$(cat docker/.profile)
  rm docker/.profile || true
  if [[ "$profile" == "local" ]]; then
    if [[ {{os()}} == "macos" ]]; then
      docker compose --profile local --profile macos -f docker/docker-compose.yml down
    else
      docker compose --profile local --profile linux -f docker/docker-compose.yml down
    fi
  elif [[ "$profile" == "microk8s" ]]; then
    if [[ {{os()}} == "macos" ]]; then
      docker compose --profile microk8s --profile macos -f docker/docker-compose.yml down
    else
      docker compose --profile microk8s --profile linux -f docker/docker-compose.yml down
    fi
  else
    echo "No profile setted"
  fi

# Remove docker data volumes
clean-docker-compose-data:
  sudo rm -Rf docker/data
  mkdir -p docker/data/.kafka
  chmod -R 777 docker/data

crud:
  sbt -Dactive-app=crud-http

tracing-http:
  sbt -Dactive-app=tracing-http

tracing-grpc:
  sbt -Dactive-app=tracing-grpc

event-sourced-ports-run seconds_to_wait='0':
  #!/usr/bin/env bash
  set -euxo pipefail
  sleep {{seconds_to_wait}}
  echo "Starting event-sourced-ports"

event-sourced-grpc-run seconds_to_wait='0':
  #!/usr/bin/env bash
  set -euxo pipefail
  sleep {{seconds_to_wait}}
  echo "Starting a cluster node using port $AKKA_CLUSTER_APP_PORT"
  sbt -Dactive-app=event-sourced-grpc -Dconfig.file=00-systems/event-sourced/02-grpc-akka/src/main/resources/application-dev.conf r

event-sourced-grpc:
  sbt -Dactive-app=event-sourced-grpc -Dconfig.file=00-systems/event-sourced/02-grpc-akka/src/main/resources/application-dev.conf

basic:
  sbt -Dactive-app=basic
