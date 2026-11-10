#!/usr/bin/env bash
# Hook preToolUse : refuse toute modification des documents de référence
# (expression de besoin et spécifications) par un outil d’édition de l’agent.
# Entrée : JSON du hook sur stdin (toolName, toolArgs).
# Sortie : un seul objet JSON de décision, ou rien pour laisser passer.

input="$(cat)"

tool="$(printf '%s' "$input" | grep -o '"toolName"[[:space:]]*:[[:space:]]*"[^"]*"' | head -n 1 | sed 's/.*"\([^"]*\)"$/\1/')"

case "$tool" in
  edit|create|str_replace_editor|apply_patch) ;;
  *) exit 0 ;;
esac

for reference in docs/specifications.md docs/edb.md; do
  if printf '%s' "$input" | grep -q "$reference"; then
    printf '{"permissionDecision":"deny","permissionDecisionReason":"%s est un document de référence : il ne doit pas être modifié par l’agent. Proposer le changement à un humain."}\n' "$reference"
    exit 0
  fi
done

exit 0
