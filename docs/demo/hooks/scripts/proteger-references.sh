#!/usr/bin/env bash
# Hook preToolUse (Copilot CLI) : refuse toute modification des documents de
# référence (expression de besoin et spécifications) par un outil d’édition.
# Entrée : JSON du hook sur stdin (toolName, toolArgs).
# Sortie : un seul objet JSON de décision, ou rien pour laisser passer.
# Seul le fichier ciblé est examiné, pas le contenu écrit : modifier le README
# pour y citer docs/specifications.md reste autorisé.

input="$(cat)"

tool="$(printf '%s' "$input" | grep -o '"toolName"[[:space:]]*:[[:space:]]*"[^"]*"' | head -n 1 | sed 's/.*"\([^"]*\)"$/\1/')"

case "$tool" in
  edit|create|str_replace_editor|apply_patch) ;;
  *) exit 0 ;;
esac

# Fichiers ciblés : champ "path" (objet ou chaîne JSON échappée) et en-têtes
# de patch « *** Update File: … », « *** Add File: … », « *** Delete File: … ».
targets="$(
  {
    printf '%s' "$input" | grep -oE '\\?"path\\?"[[:space:]]*:[[:space:]]*\\?"[^"\\]*' | sed -E 's/.*"//'
    printf '%s' "$input" | grep -oE '\*\*\* (Update|Add|Delete) File: [^"\\]*' | sed -E 's/^\*\*\* [A-Za-z]+ File: //'
  }
)"

while IFS= read -r target; do
  [ -z "$target" ] && continue
  for reference in docs/specifications.md docs/edb.md; do
    case "$target" in
      "$reference"|*/"$reference")
        printf '{"permissionDecision":"deny","permissionDecisionReason":"%s est un document de référence : il ne doit pas être modifié par l’agent. Proposer le changement à un humain."}\n' "$reference"
        exit 0
        ;;
    esac
  done
done <<< "$targets"

exit 0
