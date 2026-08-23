# Modrinth release copy

Project: [`biometunes`](https://modrinth.com/project/biometunes), id `7ASe2LUq`.

`description.md` and `changelog-<version>.md` are build inputs. `apps/mod/build.gradle.kts`
publishes them on every Modrinth upload — the `modrinth` task is finalized by
`modrinthSyncBody`, which is the only task that updates the project page — and
`verifyReleaseCopy` runs during `check`, so a missing or empty file fails the build rather
than a half-finished release.

| File | Modrinth field |
|---|---|
| `description.md` | Project page body, republished on every upload via `syncBodyFrom`. Edit here, not in the web editor, or the next release overwrites it. |
| `changelog-<version>.md` | Changelog for that version. Modrinth attaches one per version, so every release needs a new file. |
| `icon.png` | Project icon. Applied manually — nothing in the build uploads it, as Minotaur has no icon support. |

Set-once settings — summary, categories, license, links — are managed in the Modrinth web
UI and applied as of 2026-08-23. They do not change with a release.

## Project type and environment

Modrinth derives both from the **first uploaded version**, not from project settings:
uploading the Fabric JAR sets the type to **Mod**, and Modrinth reads
`"environment": "client"` from `fabric.mod.json` to derive **Client: required / Server:
unsupported**. The v2 `client_side`/`server_side` fields are accepted and silently ignored,
so neither can be pre-set. Confirm both on the project page after the first upload.

## Blocking gates

Unresolved. Do not publish until each is cleared.

1. **The [manual audio checklist](manual-audio-checklist.md) has never been run.** All 17
   rows are pending human verification; nobody has heard this mod play. Automated tests and
   a clean client startup do not substitute.
2. **Repository secrets** `MODRINTH_TOKEN` and `MODRINTH_PROJECT_ID` (`7ASe2LUq`) must exist
   in GitHub Actions.
3. **`RELEASE_APPROVED_TAG`** must equal the exact tag name; the release workflow is
   fail-closed on it.

## Releasing

1. Clear every gate above.
2. Bump `mod_version` in `gradle.properties` — the single source of truth. Add a matching
   `changelog-<version>.md` here and a `CHANGELOG.md` entry.
3. `./gradlew clean build packageSoundpack verifyDistributionArchives`
4. Set `RELEASE_APPROVED_TAG`, push the tag, unset it once the release completes.
5. Confirm the derived project type and environment on the project page.
6. The project is still `private`. Switch it to public in the web UI when the listing looks
   right; Modrinth then queues it for moderation review.

Pre-1.0 versions publish to the **beta** channel, derived from the version string. Only the
mod JAR goes to Modrinth; the soundpack ZIP is attached to the GitHub release, because a
resource pack on a mod listing confuses the install path.
