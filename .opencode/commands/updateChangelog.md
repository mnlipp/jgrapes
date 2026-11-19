---
description: Update the top `Unreleased` list in `CHANGELOG.md`
agent: build
---

# Changelog Skill

Update the top `Unreleased` list in `CHANGELOG.md` from commits after the
latest release.

### Workflow

  1. Find the latest release base:
     - Prefer the latest reachable tag: `git describe --tags --abbrev=0`.
     - If there are no tags, search recent history for a release commit and
       use that hash.
  2. Get commits: `git log <base>..HEAD --reverse --format='%H%x09%s'`.
  3. Use `https://github.com/mnlipp/jgrapes` as the repo URL for
     linking purposes.
  4. For each commit, add one concise changelog line:
     - Format: `- <one-line summary> ([<short-hash>](<url>), ...)`
     - Don't simply use the commit message as one line summary. Analyse the changes and check
       if there is a better summary.
     - If the commit clearly relates to a PR, include a link to the PR.
     - Detect PRs from subjects like `(#123)`, merge commits.
  5. Insert the new lines under the appropriate subsection (`### Added`,
     `### Changed`, `### Fixed`) in the top `Unreleased` section in
     `CHANGELOG.md`.
     - If no `Unreleased` section exists, create one at the top with the
       standard subsections.
     - Do not create a dated release heading unless the user asks for a release.

### Rules

  - Preserve the existing changelog style when obvious.
  - Always add entries to the top `Unreleased` list.
  - Skip noisy commits only when clearly non-user-facing and explain what was
    skipped.
  - Do not invent PR links.
  - Line length should not exceed 80 characters.
  - Use a four character indentation pattern for markdown enumerations
    ("  - ", not "- " nor " - ").
