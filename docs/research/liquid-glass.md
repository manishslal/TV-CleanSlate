# Liquid Glass without a heavy GPU

A review of [glisse](https://github.com/0kzh/glisse) (MIT, a web library that copies iOS 26 glass), and what Home can take from it on TVs that are slow.

No glisse code is used here. Only its measured numbers and ideas.

## How glisse draws glass

| Part | What it does | Cost |
|---|---|---|
| Lens | An SVG displacement map bends the content under the glass. Red bends further than blue, so edges split colour a little | Redrawn whenever the lens moves. Its own notes say Safari takes about 40 ms a frame on content 1200 by 800. A lens that stays still costs nothing after its first frame |
| Rim light | A picture drawn once per size and corner radius, then reused. A line about 1.2 px wide, brightest at the edge. Light comes from the top left, so the left and top sides are lit and the bottom and right get a fainter reflection. It fades over the corners. A 0.5 px edge line sits under it | One image per size. Free after that |
| Body | Backdrop blur of only 3 px with `saturate(1.5)`, because the lens does the rest. Menus use `blur(22px) saturate(1.9)`. Dark tint `rgb(40 40 40 / .62)`, light tint `rgb(255 255 255 / .6)` | The blur is the cost |
| Press | Springs, not fixed times. A mouse press: stiffness 417.6, damping 29.2, growing the long side 6 px. Release: 230 and 23.7, which overshoots slightly and settles. A finger grows 15 px and bounces more | Cheap |
| Pressed light | A faint 240 px radial highlight that follows the press and fades in 220 ms | Cheap |

## What Home already has

- **Android 13 and later:** a lens shader (`LiquidLens.kt`) that bends the wallpaper only, splits red and blue a little, and lights the top-left edge, with a fainter bottom-right one. It is drawn into a layer the GPU keeps, so it runs only when a panel moves or the picture changes. That is glisse's own trade-off.
- **Older Android:** `glassEdge` bakes a lit edge once per size, so it works on every TV.
- **LG and Samsung:** no backdrop blur at all. The wallpaper's blurred copy is baked ahead of time as frames, so the TV never blurs anything while it runs.

## What to take, in order of cost

1. **Lit left and top edges on the web.** Done for the dock, panels, pill and glass tiles as two inset rings. About free, works on the 2017 Samsung's engine. 
2. **Saturate the baked frost.** Glisse's glass is richer because it saturates the picture behind it by 1.5 to 1.9. The web's frames are baked by `webos/bake_frost.py`, so a saturation step there costs nothing at run time. Android's panel tint (`PanelTint` in `LiquidGlass.kt`) is worth checking for the same. Done: `bake_frost.py` saturates new sheets and, once, the sheets already baked.
3. **A small bounce on release.** Glisse's damping ratio is about 0.75, so a button overshoots a little and settles. Android's `Pressable` uses a ratio of 1, which never overshoots. A ratio near 0.75 on round buttons and tiles costs nothing, but it changes how focus feels everywhere, so it needs a look on a TV first. On the web, `cubic-bezier(.3,1.4,.5,1)` on focus transforms is the nearest thing.
4. **A faint radial highlight while pressed.** One gradient, shown only on press.
5. **Skip animation when the screen is hidden or reduced motion is on.** Home already collapses motion to fades for reduced motion.

## What not to bring to slow TVs

- **Displacement-map refraction on the web.** It filters the whole content each time a lens moves, on the CPU in some engines. The 2017 Samsung runs Chromium M47 and the 2018 one is about 1 GB short on memory, so it would stutter.
- **Backdrop blur that updates every frame.** The frames baked ahead of time are the cheaper way to get the same look.
- **Dispersion on small controls.** Too small to see, and it costs a pass.

## What changed to match glisse (Home 1.67-mh28)

- **Tint:** dark glass keeps 0.38 of the picture under `rgb(40 40 40)` at 0.62, as glisse does. What's behind is saturated 1.5 first. Light glass keeps 0.55 under a soft white at 0.45, clearer than glisse's 0.6, because the owner wanted Light less bright.
- **Rim:** 1.22 dp at 0.13 on dark glass and 0.3 on light, over a 0.5 dp outline at 8%. It fades at the top-right and bottom-left corners on Android; the web draws an even ring.
- **Springs:** buttons press with glisse's 417.6 stiffness at a damping ratio of 0.71, and release with 230 at 0.78, which overshoots a little. Tile focus stays critically damped, because an overshoot made the focus ring shimmer.
- **Dock:** the plate keeps 0.84 of the saturated frost and adds only a little white, in both appearances.
- **Light:** the wallpaper is dimmed 12% in Light, as Dark dims it 30%, on Android and the web.
- **Web frost:** baked with the 1.5 saturation, so the TV does no extra work.

## Where it matters most

Android 13 and later already has the full effect, so the gains are on the other three: the web's lit edge (item 1), saturated frost (2), and the bounce (3).
