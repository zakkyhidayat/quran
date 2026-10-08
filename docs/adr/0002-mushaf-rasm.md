# ADR 0002: One mushaf style, Uthmani Madani

- Status: accepted
- Date: 2026-10-08

## Context

Readers use different printed editions: the Madani mushaf (KFGQPC, Uthmani script), the IndoPak script common in South
Asia and Indonesia, the Indonesian Standard Mushaf (Kemenag), Shamarly, and others. Each has its own letter shapes,
marks, and page layout. Memorisation depends on where an ayah sits on the page, so mixing or reflowing layouts is not
acceptable.

## Decision

Support exactly one edition: the Madani mushaf, Uthmani script, riwayah Hafs 'an 'Asim, 604 fixed pages of 15 lines.
Other page styles are out of scope, and the app says so in its README and onboarding.

## Alternatives

- **Several selectable mushafs:** more users, but several times the data, fonts, testing, and verification work for a
  single maintainer, and a higher risk of content errors.
- **Reflowing text:** simpler rendering, but breaks the page memory that readers rely on.

## Consequences

- A focused, consistent reading experience, and one source to verify (ADR 0001).
- Readers who use IndoPak or the Indonesian Standard Mushaf are better served by other apps.
- Ayah-with-translation views use Unicode Hafs text, still Uthmani, but are not page facsimiles.
