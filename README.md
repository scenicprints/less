# Less

A home screen for a Pixel 11 Pro XL, as if Braun had made the phone. Kotlin, Jetpack Compose, one
module, nothing leaves the phone except a check of this repository's releases for a newer build.

The design is written down in [DESIGN.md](DESIGN.md). If a screen shows something that file does
not describe, the screen is wrong.

## What it does

**The home.** The date as a legend, the clock, the battery as a tuning scale with an orange
pointer, then pages of icons straight on the ground, no labels, no pads. Below the pages a dial:
a scale with one detent per page and the pointer geared to it. Drag the pointer or swipe the
pages, they move together. Then the dock, a fixed row of up to five, and the pull.

**Icons.** Every app on the home gets an icon drawn for it, true to the app in its own colours,
flat and geometric on one grid. They live in [icons](icons/README.md): one Python file is the
source of truth and writes the SVGs, the Android drawables and the index the launcher reads. An
app without a drawn icon shows its stock one.

**Arranging.** Hold an icon and it lifts; carry it to a cell, to the edge for the next page, or
to the dock. Drop it on another icon and they become a deck, a short pile with the next icon's
edge showing. Tap a deck and its apps slide out beneath on a hairline track. Widgets sit in the
grid and are carried the same way.

**The drawer.** Swipe up, or pull the line at the bottom. The only place with names: search as
the first line, letters as legends, one app per row. On the right a knob, a rotary dial mostly
off the edge with the alphabet round its rim and a fixed orange index. It turns as the list
scrolls; drag its rim and the list follows. Tap search and a panel appears with FREQUENT, RECENT
and NEW, two rows of five.

**Holding an icon.** The app's own shortcuts, then app info, notifications, Play Store,
uninstall, rename, change icon, hide, add to or remove from the home, and its widgets.

**Settings.** Hold the empty ground. The colourway (black or white Braun, chosen once, never
switched for you), making Less the home, apps hidden from the drawer, and updates.

## Building

There is no Android SDK on the machine this is written on. The GitHub workflow in
`.github/workflows/build.yml` builds, tests and publishes a release APK on every push to `main`,
tagged `v<run number>`, which is also the version code the in-app updater compares against.
Secrets `LESS_KEYSTORE_BASE64`, `LESS_STORE_PASSWORD`, `LESS_KEY_ALIAS` and `LESS_KEY_PASSWORD`
sign it; without them the APK is debug-signed and still installs.

To build locally with a JDK 17 and the Android SDK installed:

```powershell
gradle :app:testReleaseUnitTest :app:assembleRelease
```

## Layout of the code

- `data` — the layout model (pages, cells, decks, the dock) and its JSON store; settings.
- `apps` — the list of launchable apps, search and grouping, usage for the search panel.
- `icons` — the icon index, read from `assets/icons.json`.
- `home`, `drawer`, `menu`, `settings` — the four screens.
- `widget` — the app widget host.
- `ui` — the palette, type, parts (rule, label, scale, choice, action, pull), motion, gestures.
- `update` — the GitHub releases updater.

The layout is one readable file, `layout.json`, in the app's files directory.
