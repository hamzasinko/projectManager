#!/bin/bash
if [ -n "$ALLOW_FILTERS" -o -n "$DENY_FILTERS" ]; then
    FILTERS="-Drecorder.filters.enable=true"
else
    FILTERS=""
fi
if [ -n "$ALLOW_FILTERS" ]; then
    FIDX=0
    for FILTER in ${ALLOW_FILTERS}; do
        FILTERS="${FILTERS} -Drecorder.filters.allowList.${FIDX}=${FILTER}"
        ((FIDX++))
    done
fi
if [ -n "$DENY_FILTERS" ]; then
    FIDX=0
    for FILTER in ${DENY_FILTERS}; do
        FILTERS="${FILTERS} -Drecorder.filters.denyList.${FIDX}=${FILTER}"
        ((FIDX++))
    done
fi

ADDITIONAL_ARGS=""
for ARG in "$@"; do
    echo $ARG
    ADDITIONAL_ARGS="${ADDITIONAL_ARGS} -D${ARG}"
done

COMMAND="gatling:recorder \
    -Drecorder.core.headless=true \
    -Drecorder.proxy.port=3128 \
    -Dgatling.recorder.className="${CLASSNAME}" \
    -Dgatling.recorder.package="${PACKAGE}" \
    ${FILTERS} \
    ${ADDITIONAL_ARGS}"

echo "Running $COMMAND"

/app/mvnw $COMMAND