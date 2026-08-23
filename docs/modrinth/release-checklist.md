# Publishing BiomeTunes on Modrinth

The live project is [`biometunes`](https://modrinth.com/project/biometunes), id `7ASe2LUq`,
owned by the `hampfh` Modrinth account.

Project-level copy and metadata are versioned in this directory and are the source of
truth. Do not edit them in the Modrinth web editor — edit the files here and re-apply,
or the next release will overwrite your changes (`syncBodyFrom` in
`apps/mod/build.gradle.kts` republishes `description.md` on every upload).

| File | Modrinth field |
|---|---|
| `summary.txt` | Summary (the one-line blurb under the title) |
| `description.md` | Description (the project page body) |
| `changelog-<version>.md` | Changelog for that specific version |
| `project-settings.json` | Categories, links, license, slug, icon |
| `icon.png` | Project icon — the BiomeTunes logo at 400×400, within Modrinth's 256 KiB icon limit |

## Applying project metadata

```bash
MODRINTH_TOKEN=mrp_... docs/modrinth/apply-project-settings.sh
```

This touches project settings only. It never uploads a version and never changes
publication status.

## Current state

Applied to the draft on 2026-08-23:

- Summary, description body, icon, `Apache-2.0` license, source and issue links.
- Categories `decoration`, `utility`; additional category `adventure`.
- Status remains `draft` / `requested_status: private`. Nothing is public.

## Project type and environment

Modrinth no longer takes these as project settings — both are **derived from the first
uploaded version**:

- Uploading the Fabric JAR sets the project type to **Mod**.
- Modrinth reads `"environment": "client"` from `fabric.mod.json` and derives
  **Client: required / Server: unsupported**.

The v2 `client_side`/`server_side` fields are accepted and ignored by the API, so they
cannot be pre-set. **After the first upload, open the project page and confirm the
derived environment is correct**; if it is not, fix it in the web UI.

## Blocking gates before anything is published

These are unresolved. Do not publish until each is cleared.

1. **All 17 rows of [`docs/testing/manual-audio-checklist.md`](../testing/manual-audio-checklist.md)
   are `PENDING HUMAN VERIFICATION`.** No human has heard this mod play. Automated tests
   and a clean client startup cannot substitute.
2. **The Forest track has no recorded composer.** `description.md` and the in-repo credits
   currently say attribution is pending and invite the composer to come forward. Confirm
   the artist and the wording you want to publish, or accept publishing with that notice.
3. **Repository secrets.** `MODRINTH_TOKEN` and `MODRINTH_PROJECT_ID` (`7ASe2LUq`) must be
   set in GitHub Actions secrets.
4. **Release approval.** The release workflow is fail-closed on the `RELEASE_APPROVED_TAG`
   Actions variable. It must be set to the exact tag name.

## Release procedure

1. Clear every gate above.
2. Confirm `mod_version` in `gradle.properties` matches the tag you intend to push, and
   that `changelog-<version>.md` exists in this directory — the `modrinth` task fails if
   it does not.
3. Verify locally: `./gradlew clean build packageSoundpack verifyDistributionArchives`.
4. Set the `RELEASE_APPROVED_TAG` Actions variable to the tag name, e.g. `v0.5`.
5. Push the tag. The workflow verifies tag-vs-version, verifies approval, builds, creates
   the GitHub release, and runs `:apps:mod:modrinth`.
6. Confirm the derived project type and environment on the project page.
7. The project is still `private`. Switch it to **public** in the Modrinth web UI when you
   are satisfied with the listing; Modrinth then queues it for moderation review.
8. Unset `RELEASE_APPROVED_TAG`.

## Notes

- Pre-1.0 versions publish as Modrinth release channel **beta**; `1.x` and later publish as
  **release**. This is derived from the version string in `apps/mod/build.gradle.kts`.
- Only the mod JAR goes to Modrinth. The standalone soundpack ZIP is attached to the
  GitHub release, because a resource pack on a mod listing confuses the install path.
- A gallery is not configured. Screenshots are of limited use for an audio mod; a short
  video linked from the description would serve better if you want one.
