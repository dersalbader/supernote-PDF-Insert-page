# InsertPage — Supernote plugin (scaffold)

Adds a blank page into the PDF you're reading, right after your current
page, in one tap — without leaving the reader or touching a computer.

## Honest status

This is a working scaffold, not a tested plugin. Two things are solid:

- The manifest (`PluginConfig.json`) and permission format — matches a
  real, published, working Supernote plugin (FrameClip).
- The actual PDF page insertion logic (`App.tsx`), using `pdf-lib` —
  this is standard and correct for inserting a blank page into a PDF.

One thing is **not yet verified** — marked with `TODO` in
`InsertPageNativeModule.java`:

- How to ask the Supernote reader "which PDF and page am I on right
  now", and how to tell it "reload this file and jump to page N".
  FrameClip's plugin does this successfully, so the real API call
  exists in the SDK — I just haven't seen FrameClip's own native
  module source to copy the exact method names.

## What to do with this

1. Set up the toolchain (Node + Android Studio — done).
2. Clone FrameClip's repo alongside this one:
   `git clone https://github.com/taoist22/sn-frameclip`
3. Open
   `sn-frameclip/android/app/src/main/java/com/snframeclip/FrameClipNativeModule.java`
   and find the method(s) it uses to get the current document path and
   page index. Copy that pattern into the two TODO methods in
   `InsertPageNativeModule.java` here.
4. Run:
   ```
   npm install
   ./buildPlugin.sh
   ```
5. Copy `build/outputs/InsertPage.snplg` onto your Supernote (USB, or
   however you moved files before) and install it via
   Settings → Apps → Plugins → Add Plugin.
6. Test on a throwaway PDF copy first, not an original you care about,
   in case the write-back step needs adjusting.

## If step 3 doesn't turn up a clean match

Fall back plan: instead of editing the open PDF live, have the plugin
save the new (page-inserted) PDF as a copy next to the original, then
show a message telling you to reopen that copy. Slightly less seamless
than "insert and stay in place", but removes the live-reload unknown
entirely. Happy to build that version instead if the live-reload
approach turns out not to be exposed to plugins.
