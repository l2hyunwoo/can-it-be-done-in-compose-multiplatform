# Can it be done in Compose Multiplatform?

A collection of graphics & animation demos rebuilt in **Compose Multiplatform**, inspired by
the ["Can it be done in React Native?"](https://youtube.com/playlist?list=PLkOyNuxGl9jxB_ARphTDoOWf5AE1J-x1r&si=k0127eYccOuFjPgR) series — but running on **Android, iOS, and Desktop**
from a single Kotlin codebase (wasmJs added per-demo where the graphics APIs allow).

Each demo is a **self-contained CMP project** in its own directory, so you can open, build, and
run any one of them independently.

## Demos

| Demo | Android | iOS | Desktop | wasmJs |
|------|:-------:|:---:|:-------:|:------:|
| [rainy-window](rainy-window/)| ✅ | ✅ | ✅ | — |

### Rainy Window

<video src="https://github.com/user-attachments/assets/e4454093-d7c0-40db-a7c1-5740460d9e41" />

## Credits

**rainy-window** — the raindrop field is an AGSL/SKSL port of **"Heartfelt"** by
Martijn Steinrucken (BigWings), <https://www.shadertoy.com/view/ltffzl>, licensed
**CC BY-NC-SA 3.0**. The ported shader — and therefore that demo — is
**non-commercial** and **share-alike** under the same license. Do not use it
commercially. The rainy-night background photo is provided by the repo author.
