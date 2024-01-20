set shell := ["sh", "-c"]
set allow-duplicate-recipes
set positional-arguments
set dotenv-path := ".local/.env"
set dotenv-load := true
set export

postgres_sql_file := justfile_directory() + "/docker/postgres/docker-entrypoint-initdb.d/postgres.sql"
postgres_data_dir := justfile_directory() + "/docker/data/.postgres"
cassandra_data_dir := justfile_directory() + "/docker/data/.cassandra"

alias dc-u := docker-compose-up
alias dck8s-u := docker-compose-k8s-up
alias dc-d := docker-compose-down
alias clean-dd := clean-docker-compose-data

[private]
api-setup:
  sbt generateSmithyFromOpenApi

run-migrations:
    liquibase update --defaults-file=docker/cassandra/liquibase.properties
    liquibase update --defaults-file=docker/postgres/liquibase.properties

truncate-all:
    liquibase execute-sql \
              --sql-file=docker/cassandra/ddls/truncate-all.cql \
              --defaults-file=docker/cassandra/liquibase.properties
    liquibase execute-sql \
              --sql-file=docker/postgres/ddls/truncate-all.sql \
              --defaults-file=docker/postgres/liquibase.properties
drop-all:
    liquibase drop-all \
              --defaults-file=docker/postgres/liquibase.properties

[private]
docker-compose-k8s-up:
  #!/usr/bin/env bash
  set -euxo pipefail
  just docker-compose-up microk8s

docker-compose-up environment='local': 
  #!/usr/bin/env bash
  set -euxo pipefail

  if [[ ! -d "{{cassandra_data_dir}}" ]]; then
    showLogs=true
  else
    showLogs=false
  fi
  
  if [ -f docker/.profile ]; then
    profile=$(cat docker/.profile)
    if [ "$profile" == "microk8s" ]; then
      if [[ {{os()}} == "macos" ]]; then
        docker compose --profile $environment --profile macos -f docker/docker-compose.yml down
      else
        docker compose --profile $environment --profile linux -f docker/docker-compose.yml down
      fi
      if [[ {{os()}} == "macos" ]]; then
        docker compose --profile $environment --profile macos -f docker/docker-compose.yml up -d
      else
        docker compose --profile $environment --profile linux -f docker/docker-compose.yml up -d
      fi
    fi
  else
    echo $environment > docker/.profile
    if [[ {{os()}} == "macos" ]]; then
      docker compose --profile $environment --profile macos -f docker/docker-compose.yml up -d
    else
      docker compose --profile $environment --profile linux -f docker/docker-compose.yml up -d
    fi
  fi

  if [ "$showLogs" == "true" ]; then
    docker logs docker-cassandra_temp-1 -f
    just run-migrations
  fi

docker-compose-down:
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

clean-kafka-data:
  sudo rm -Rf docker/data/.kafka
  mkdir -p docker/data/.kafka
  chmod -R 777 docker/data/.kafka
  
crud:
  sbt -Dactive-app=crud-rest

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

#io.r2dbc.postgresql


event-sourced-grpc:
  sbt -Dactive-app=event-sourced-grpc -Dconfig.file=00-systems/event-sourced/02-grpc-akka/src/main/resources/application-dev.conf

basic:
  sbt -Dactive-app=basic-rest

crud-gen-avsc:
  #!/usr/bin/env bash
  set -euxo pipefail

  allAvdls="$AVRO_IDLS_SOURCE/*.avdl"
  for file in $allAvdls; do
    dfile=$(basename $file)
    dfile=${dfile%?????}
    destination="$AVRO_SCHEMAS_BASE_PATH/$dfile"
    mkdir -p $destination
    avro-tools idl2schemata \
        $file \
        $destination
    load-schemas-to-registry.scala --schema-subject $dfile --path-of-schemas $destination
  done

keystore_file := justfile_directory() + "00-systems/smithy-basic/02-http-service-basic/src/main/resources/server.jks"

gen-keystore:
  mkcert -pkcs12 -p12-file example.com.p12 example.com '*.example.com'
  rm {{keystore_file}} || true

  keytool -importkeystore \
          -deststorepass changeit \
          -destkeypass changeit \
          -deststoretype pkcs12 \
          -srckeystore example.com.p12 \
          -srcstoretype PKCS12 \
          -srcstorepass changeit \
          -destkeystore 00-systems/smithy-basic/02-http-service-basic/src/main/resources/server.jks

  rm example.com.p12

rm-keystore:
  rm {{keystore_file}} || true

[private]
show:
  #!/usr/bin/env bash
  set -euxo pipefail

  export DENV=2
  direnv reload
  echo $AKKA_CLUSTER_APP_PORT

[private]
generate-avros:
  rm -Rf 00-apis/integration/cruds/00-api-kafka-to-publish/avro
  mkdir -p 00-apis/integration/cruds/00-api-kafka-to-publish/avro
  avro-tools idl2schemata 00-apis/integration/cruds/00-api-kafka/avro/Advertiser.avdl 00-apis/integration/cruds/00-api-kafka-to-publish/avro

# https://www.freecodecamp.org/news/sort-dictionary-by-value-in-python/#howtosortadictionarywiththesortedmethod
[private]
clean-avros: generate-avros
  #!/usr/bin/env python

  import json
  import glob
  import os

  for filename in glob.glob('00-apis/integration/cruds/00-api-kafka-to-publish/avro/*.avsc'):
    with open(filename, "r+") as f, open(filename + "_", "w") as f2:
      data = json.load(f)
      if 'fields' in data.keys():
        for a in data['fields']:
          if isinstance(a['type'], dict):
            a['type'] = a['type']['name']
      f2.write(json.dumps(data, indent=2))
    os.remove(filename)
    os.rename(filename + "_", filename)

[private]
grpcui-func-start port='8080':
  #!/usr/bin/env bash
  set -euxo pipefail
  grpcui -plaintext 0.0.0.0:$port &
  echo "grpcui started on port $port"
  echo $! > .grpcui-$port.pid
  pid=$(cat .grpcui-$port.pid)
  path=$(sudo netstat -ltnp | grep -w $pid | awk '{print $4}')
  chrome "http://$path/"

[private]
grpcui-func-end port='8080':
  #!/usr/bin/env bash
  set -euxo pipefail
  kill -9 $(cat .grpcui-$port.pid)
  rm .grpcui-$port.pid

grpcui-s:
  just grpcui-func-start 8080
  just grpcui-func-start 8081

grpcui-e:
  #!/usr/bin/env bash
  just grpcui-func-end 8080
  just grpcui-func-end 8081
