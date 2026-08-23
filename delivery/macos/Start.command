#!/bin/bash
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
"$SCRIPT_DIR/scripts/configurator.sh" start
status=$?
if [ -t 0 ]; then printf '\nНажмите Enter для закрытия…'; IFS= read -r _; fi
exit "$status"
