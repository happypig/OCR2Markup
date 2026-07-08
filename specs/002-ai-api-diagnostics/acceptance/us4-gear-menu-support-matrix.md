# US4 Acceptance Matrix: Gear Menu Support Visibility

## Scope

Validate the existing gear icon menu contract for `Preferences...`, `User Manual`, and `About`, plus the shared packaged release-notes behavior used by the About dialog and published extension metadata.

## Scenarios

| Scenario | Given | When | Then |
|---|---|---|---|
| Menu order | The plugin is installed and the DAMA panel is visible | The user opens the existing gear icon menu | The menu items appear in this order: `Preferences...`, `User Manual`, `About` |
| User manual navigation | The gear icon menu is open | The user activates `User Manual` | The plugin opens `https://docs.google.com/document/d/1JHWAu4KJ6eb-UZhh-uYW8HbzsKc6fD5i_lVKTQWj9HQ/edit?usp=sharing` |
| About dialog content | Packaged build metadata and release notes are available | The user activates `About` | The dialog shows the installed plugin version and the full current release notes loaded from the packaged shared source |
| Release-notes fallback | A local or development build is missing or cannot parse the packaged release-notes resource | The user activates `About` | The dialog still shows the installed plugin version and a localized fallback message instead of failing |

## Notes

- `Preferences...` continues to reuse the existing `menuItem.preferences` translation key and options-page wiring.
- The shared release-notes source is `src/main/resources/release-notes.xhtml`.
- The extension descriptor generation path must consume the same release-notes source instead of maintaining a separate copy of the release notes.
