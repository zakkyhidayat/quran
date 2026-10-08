# ADR 0003: Native Android with Kotlin and Jetpack Compose

- Status: accepted
- Date: 2026-10-08

## Context

The app must render KFGQPC V4 colour fonts (COLR/CPAL, one font per page) precisely, select their built-in palettes for
light and dark themes, and follow Material 3 Expressive closely. It targets Android only.

## Decision

Kotlin with Jetpack Compose and Material 3 (Expressive APIs), minSdk 26. Data access through a read-only SQLite
database behind one repository; settings in DataStore. No Google Play Services and no third-party SDKs.

## Alternatives

- **Flutter or React Native:** cross-platform, but colour-font palettes, per-glyph text measurement, and the newest
  Material 3 Expressive components are less direct or unavailable; a second platform is not a goal.
- **Android Views:** stable, but Material 3 Expressive and adaptive layouts are Compose-first.

## Consequences

- Full control over glyph rendering (palette-patched fonts, canvas drawing, background text layout).
- Material 3 Expressive components are alpha APIs; library updates may need code changes.
- No GMS keeps the app suitable for devices without Google services and for F-Droid, once the build is reproducible.
