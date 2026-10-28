## Change summary

<!-- Describe what changed and why. -->

## Verification checklist

- [ ] Confirm new or changed dependency coordinates and versions exist in the official repository.
- [ ] Keep dependency versions centralized in `gradle/libs.versions.toml` where practical.
- [ ] Run the Android dependency metadata check and debug build.
- [ ] Run unit tests and review the complete GitHub Actions result.
- [ ] Do not merge while required checks are pending or failing.
- [ ] If persistence/schema changes are included, consider existing user data, migrations, and rollback behavior.
- [ ] If UI changes are included, confirm the intended screen is actually wired into the app entry point.

## Testing notes

<!-- List commands/checks run and any remaining limitations. -->
