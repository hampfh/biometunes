# Publishing BiomeTunes on Modrinth

Project: [`biometunes`](https://modrinth.com/project/biometunes), id `7ASe2LUq`, owned by
the `hampfh` account.

## What lives here

| File | Modrinth field |
|---|---|
| `description.md` | Project page body. Republished on every upload via `syncBodyFrom`, so edit it here, not in the web editor. |
| `changelog-<version>.md` | Changelog for that specific version. Modrinth attaches a changelog per version, so each release needs one. |
| `icon.png` | Project icon. |

Everything else — summary, categories, license, links — is set once in the web UI and was
applied on 2026-08-23. It is deliberately not tracked here; it never changes with a release.

## Project type and environment

Modrinth derives both from the **first uploaded version**, not from project settings:

- Uploading the Fabric JAR sets the project type to **Mod**.
- Modrinth reads `"environment": "client"` from `fabric.mod.json` to derive
  **Client: required / Server: unsupported**.

The v2 `client_side`/`server_side` fields are accepted and silently ignored, so neither can
be pre-set. **After the first upload, confirm the derived values on the project page** and
correct them in the web UI if they are wrong.

## Blocking gates

Unresolved. Do not publish until each is cleared.

1. **All 17 rows of [`manual-audio-checklist.md`](../testing/manual-audio-checklist.md) are
   `PENDING HUMAN VERIFICATION`.** Nobody has heard this mod play. Automated tests and a
   clean client startup do not substitute.
2. **The Forest track has no recorded composer.** `description.md` says attribution is
   pending and invites the composer to come forward. Confirm the wording, or accept
   publishing with that notice.
3. **Repository secrets** `MODRINTH_TOKEN` and `MODRINTH_PROJECT_ID` (`7ASe2LUq`) must exist
   in GitHub Actions.
4. **`RELEASE_APPROVED_TAG`** must be set to the exact tag name; the workflow is fail-closed
   on it.

## Releasing

1. Clear every gate above.
2. Bump `mod_version` in `gradle.properties` — the single source of truth. Add a matching
   `changelog-<version>.md` here and a `CHANGELOG.md` entry.
3. `./gradlew clean build packageSoundpack verifyDistributionArchives`
4. Set `RELEASE_APPROVED_TAG`, push the tag, then unset it once the release completes.
5. Confirm the derived project type and environment on the project page.
6. The project is still `private`. Switch it to public in the web UI when you are satisfied;
   Modrinth then queues it for moderation review.

Pre-1.0 versions publish to the **beta** channel, derived from the version string. Only the
mod JAR goes to Modrinth; the standalone soundpack ZIP is attached to the GitHub release,
because a resource pack on a mod listing confuses the install path.
