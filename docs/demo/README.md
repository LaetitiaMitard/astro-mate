# Kit de démo Copilot

Ce dossier contient des assets Copilot prêts à l’emploi pour illustrer
l’atelier. Ils ne sont **pas actifs** par défaut : le dossier `.github/` est
ignoré par Git (à l’exception de `copilot-instructions.md`) afin que les
participants créent eux-mêmes leurs assets pendant les exercices.

Pour activer un asset pendant une démo, le copier à l’emplacement indiqué,
depuis la racine du dépôt.

| Asset | Copier vers | Rôle dans la démo |
|---|---|---|
| [`skills/enquete-regression/`](skills/enquete-regression/SKILL.md) | `.github/skills/enquete-regression/` | Solution de référence de la séquence 2, si `/create-skill` n’est pas disponible ou pour comparer avec le skill généré. |
| [`agents/revue-accessibilite.agent.md`](agents/revue-accessibilite.agent.md) | `.github/agents/` | Agent personnalisé en lecture seule pour relire la feature Orbit (séquence 4). |
| [`hooks/proteger-references.json`](hooks/proteger-references.json) et [`hooks/scripts/`](hooks/scripts/proteger-references.sh) | `.github/hooks/` | Hook `preToolUse` qui refuse toute modification de `docs/specifications.md` et `docs/edb.md` par un outil d’édition. |
| [`mcp/mcp.json`](mcp/mcp.json) | `.vscode/mcp.json` | Serveur MCP GitHub distant pour VS Code. Copilot CLI intègre déjà le serveur GitHub. |

Exemple :

```sh
mkdir -p .github/skills .github/agents .github/hooks .vscode
cp -r docs/demo/skills/enquete-regression .github/skills/
cp docs/demo/agents/revue-accessibilite.agent.md .github/agents/
cp -r docs/demo/hooks/. .github/hooks/
cp docs/demo/mcp/mcp.json .vscode/mcp.json
```

## Notes

- **Hook** : conçu pour Copilot CLI (format `version: 1`, champs `toolName`
  et `toolArgs`). Le script est en bash et n’examine que le fichier ciblé par
  les outils d’édition de l’agent (`edit`, `create`, `str_replace_editor`,
  `apply_patch`) : citer `docs/specifications.md` dans un autre fichier reste
  autorisé. Une commande shell qui modifierait ces fichiers n’est pas
  interceptée, ce qui ouvre la discussion sur la portée réelle d’un garde-fou.
  Les hooks de VS Code utilisent d’autres noms d’outils et ne sont pas couverts.
  Script testé sur des entrées d’exemple ; à valider dans Copilot CLI avant
  l’atelier.
- **Agent** : il n’a accès qu’aux outils `read` et `search`, ce qui illustre
  la limitation des permissions d’un agent spécialisé.
- **MCP** : au premier lancement, VS Code demande de s’authentifier auprès de
  GitHub. Pour l’enquête, l’incident peut être publié en issue afin que
  l’agent le lise via le serveur MCP plutôt que dans `docs/incidents.md`.
