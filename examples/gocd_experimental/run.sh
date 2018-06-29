#!/bin/bash
set -eo pipefail

readonly SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

readonly TMP_LOG="/tmp/run.$$.log"
readonly BASE_URL="http://localhost:8153/go"

wait_for_server() {
    local url=$1
    echo -n " waiting for ${url}"
    until curl --output /dev/null --silent --head --fail "$url"; do
        printf '.'
        sleep 5
    done
}

announce() {
    local text="$1"
    echo -ne "\033[1;30m"
    echo -n "$text"
    echo -ne "\033[0m"
}

hint_at_logs() {
    # shellcheck disable=SC2181
    if [[ "$?" -ne 0 ]]; then
        echo
        echo "Logs are in ${TMP_LOG}"
    fi
}

container_exists() {
    if [[ -z $(docker-compose ps -q) ]]; then
        return 1
    else
        return 0
    fi
}

goal_start() {
    if ! container_exists; then
        announce "Provisioning docker image"
        echo

        # Wonky workaround for trying to boot up server image with minimal config, but no agent registration
        mkdir -p server/config
        cp server/*.xml server/config

        docker-compose up --no-start

        echo "done"
    fi

    announce "Starting docker image"
    docker-compose up -d &> "$TMP_LOG"

    wait_for_server "$BASE_URL"
    echo " done"
    rm "$TMP_LOG"
}


goal_stop() {
    announce "Stopping docker image"
    docker-compose stop &> "$TMP_LOG"
    echo " done"
    rm "$TMP_LOG"
}

goal_destroy() {
    announce "Destroying docker container"
    docker-compose down &> "$TMP_LOG"
    echo " done"
    rm "$TMP_LOG"
}

goal_purge() {
    announce "Purging docker images"
    docker rmi gocd/gocd-server:v18.6.0 >> "$TMP_LOG"
    docker rmi gocd/gocd-agent-alpine-3.7:v18.6.0 >> "$TMP_LOG"
    echo " done"
    rm "$TMP_LOG"
}

main() {
    trap hint_at_logs EXIT

    if type -t "goal_$1" &>/dev/null; then
        "goal_$1"
    else
        echo "usage: $0 (start|stop|destroy|purge)"
    fi
}

main "$@"