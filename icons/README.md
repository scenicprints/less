# Icons

Every app on the home screen gets an icon drawn for it. The icon is true to the app, colours and
all, so it is found at a glance. What makes it Rams is the drawing.

## The rules

- Each icon is its own shape. No tile: the mark is the silhouette, so Maps is a pin, Brave is a
  shield, Chase is an octagon, Snapchat is a ghost, and a Google app that is a disc is a disc.
- Drawn on a 56 grid, inside the middle 48; 4 units of margin on each side stay clear. Shapes
  carry about the same visual mass, so a row of them reads as one family.
- Flat colour only. No gradients, no shading, no highlights, no shadows.
- Built rectangles share one corner radius, 6. Strokes are 4 units, round ends, round joins; thin
  detail is 3; a letter drawn as a shape is heavier. Nothing thinner than 3.
- Shapes are built from lines, rectangles, circles and arcs. If a brand mark cannot be said in
  those, it is simplified until it can, while staying recognisable.
- The brand's own colours, flattened to one tone each. White is the warm `#F6F2EA`, never pure
  white. Dark is `#1B1B19`, never pure black.

## What is drawn

`layout.md` is the home screen as read from the Nova backup, and the set covers every app on it:
the dock, the pages, the contents of the Home, Money, Bambu and Travel decks, and the phone's
basics so the drawer is drawn too. The four web apps pinned through Brave keep their site icons.

## Generated icons

Most icons come from an image model, prompted with `prompts.md`. Save each result as
`png/<package>.png`. The pipeline keys out the background by flooding in from the edges, so an
off-white mark inside a disc survives and whatever off-white the model chose does not matter;
softens the edge so no fringe shows on charcoal; trims and scales the mark to the same box as the
drawn icons; and writes a 256 px PNG into `res/drawable-nodpi`. A generated icon wins over the
drawn one for the same app. Fake transparency, a painted checkerboard, cannot be keyed; ask for a
plain background instead.

## The pipeline

`set.py` is the drawn set: each icon is a few lines of Python naming its package and its marks,
the fallback for anything the model fumbles. `make.py` holds the helpers and the writers, and
`png_step.py` the generated-icon step. Running `make.py` writes:

- `svg/<package>.svg`, one per app, for looking at and for the mockups.
- `../app/src/main/res/drawable/ic_<name>.xml`, an Android vector drawable per app, what the
  launcher draws.
- `../app/src/main/assets/icons.json`, the index: package name to drawable name, which the
  launcher reads at start. An app not in the index shows its stock icon.

Run it from this folder after any change:

```powershell
python make.py
```

Needs Pillow and numpy for the generated icons. The drawables use only what Android's vector
format supports: paths with fills and strokes, no masks, no gradients, no text.
