# The design language

What Less looks like, agreed on 23 November 2026, so that every screen and every line of code
can be checked against it. If something on a screen is not covered here, it probably should not
be on the screen.

## The idea

A Pixel 11 Pro XL as if Braun had made it today. Rams's discipline with a 2026 phone's habits:
thumb-reach controls, big light type, honest animation. Modern first, Braun second; never a
museum piece.

## Surfaces and lines

- One colourway at a time, chosen in settings, never switched automatically. Charcoal is the
  working one: ground `#131313` (true OLED black `#0E0E0E` is acceptable for the drawer), rules
  `#2F2F2D`, ink `#E6E4DE`, dim `#8B8984`. The white one: ground `#E9E7E1`, rules `#C4C1B9`, ink
  `#1B1B19`, dim `#6E6C66`.
- One accent, orange `#EC5B1A`. It marks the pointer on a scale and the dot on the one Nudge that
  needs you. It is never decoration.
- Hairlines of 1px divide things. No cards, no pills, no shadows, no blur, no gradients.
- Side margin 28dp. Everything aligns to it.

## Type

Inter (Roboto if no font file is present). Clock 92sp light, tabular numerals. Legends 11sp medium,
spaced 1.4, upper case, dim. Names in the drawer 20sp regular, ink. Nothing bold.

## The home

Top to bottom: date legend, clock, the battery scale with its orange pointer and a legend row
(BATTERY left, the number right), the Nudge rail, pages of icons, the dial, the dock, the pull.

- The battery scale is the one instrument. It stays. Nudge sits under it on a rail: hairlines, one
  line per nudge, orange dot on the one due now, grey dots otherwise, and no height at all when
  there is nothing to say.
- Icons sit straight on the ground. No pads, no keys, no labels, no notification dots.
- Pages change with an unlabelled dial: a scale of ticks, one detent per page, the orange pointer.
  The leftmost page holds the stock Google Calendar widget, as it is.
- The dock is a fixed row below the dial. Which of the three drawings is still to be chosen.
- The pull is a 36dp grey line centred at the bottom. Swipe up anywhere, or drag the pull, for the
  drawer. Swipe down for notifications. Hold the ground for settings.

## Icons

Every app on the home gets an icon drawn for it, kept in the icon database: package, drawing,
whether it is a deck. The icon is true to the app, colours and all, so it is found at a glance,
and it is its own shape: a pin, a shield, an octagon, a ghost, a disc. No tile behind it. What
makes it Rams is the drawing: flat, geometric, one grid, one corner radius, one stroke weight, no
gradients or shading. Apps not yet drawn show their stock icon. The rules and the pipeline are in
`icons/README.md`.

## The drawer

The only place with names. It slides up over the home as a panel with the pull on its top edge.
Search is the first line, plain text, and never opens the keyboard by itself. Tapping it opens the
keyboard and drops a panel in beneath the line: a chooser of FREQUENT, RECENT and NEW in the
settings style (the chosen word in ink with a short orange rule), and under it two rows of five
icons with small names. Frequent is the apps opened most, recent the apps opened last, new the
apps installed last. Typing replaces the panel with matches. Letters are legends
with a hairline, like the Nudge rail's heading. Apps are one per row, icon then name.

The fast scroller is a knob: a rotary dial sitting mostly off the right edge, so a third of its
rim shows. It is a machined part, not a drawing of one: a dark recess it sits in, a knurled grip
round the edge, a bevel line, a printed scale band with a major tick per letter and a minor tick
between, the letters following the rim, and an inner face. A fixed orange index mark outside the
rim points at the current letter. The knob turns as the list scrolls, with a detent at each
letter; drag the rim and the list follows. Letters past the visible arc are round the back and
come into view as it turns.

## Arranging the home

Icons drag in place and snap to the grid. Carry one across the edge and the dial pointer follows
to the next page; drag past the last page and a page is made; empty a page and it goes. Widgets
are arranged exactly like apps: they sit in the grid, span the cells they need, and drag the same
way. The Nudge widget is simply a widget placed under the battery scale. Shelving, the edit mode,
is only for the rails: what hangs above the pages and in what order.

## Decks, not folders

A deck is a stack: the icon on top with the edge of the next one showing beneath it, offset a few
pixels, so an icon with more behind it reads as a short pile. Hold it and a row slides out beneath
on a hairline track, then slides back. No folder icons, no folder names.

## Day one, not later

- An updater, as in every app of this family: Less checks its own GitHub releases and installing a
  new build is a row in settings.
- The ordinary things done properly. The home button returns to the first page and closes the
  drawer. Back closes whatever is open. Large text sizes are respected. Everything is labelled for
  TalkBack. Permissions are asked for at the moment they are needed, in a sentence, never in a
  wizard.

## Holding an icon

A plain list over the icon: the shortcuts the app itself provides, then app info, notification
settings, Play Store page, uninstall, rename (for the drawer), change icon, hide, add or remove
from the home, move to a deck, put one of its widgets on a rail. Nothing else.

## Motion

Everything moves like a mechanism. One easing, about 200ms, no bounce, no overshoot, no spring.
The drawer follows the finger and settles. Pages and the dial pointer move together. Apps open out
of their icon and back into it using Android's own animation. The battery pointer moves like a
needle, only when the number changes. Press feedback is a small sink of the icon, no ripple.

## Deliberately absent

Search on the home. A suggested apps row. Notification dots. A feed page. Folders. Labels under
icons. Automatic colourway switching. The watch face. Anything decided after a "wouldn't it be
cool if".
