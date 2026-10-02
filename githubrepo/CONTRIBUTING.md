# Contributing to Silouder

Thank you for your interest in improving Silouder! The project is licensed under
GPL-3.0, so every contribution must stay free software.

## Ground rules

- Keep it FOSS: no proprietary SDKs, trackers, analytics, or Google/Firebase services.
- Be honest: do not present simulated functionality as working. Label prototypes clearly.
- Stay privacy-first: no data collection, no permissions beyond what a feature needs.

## Workflow

1. Fork the repository and create a feature branch from `main`.
2. Make your changes with the fewest files touched that solve the problem.
3. Run the checks before opening a pull request:

   ```bash
   ./gradlew assembleDebug
   ./gradlew testDebugUnitTest
   ```

4. Open a pull request describing what changed and why.

## Code style

- Kotlin official code style (`kotlin.code.style=official`).
- Keep packages under `com.silouder.app.*`.
- Name UI test tags consistently (`feature_element_purpose`).

## Reporting bugs

Open a GitHub issue with: steps to reproduce, expected vs. actual behavior,
device/Android version, and logs if available. For security-sensitive reports,
please open an issue titled `[SECURITY]` so it gets priority review.

## Licensing

By contributing, you agree that your contributions will be licensed under the
GNU General Public License v3.0, the same license as the rest of the project.
