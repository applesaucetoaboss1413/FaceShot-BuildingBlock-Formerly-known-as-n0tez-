# Page Design – faceshot-chopshop Editor Shell (Expo Router)

## Global Styles
### Design tokens (faceshot-chopshop)
- Background: near-black base (`#0B0C10`), elevated surfaces (`#11131A`, `#171A24`)
- Text: primary (`#F4F6FF`), secondary (`#B7BDD4`), muted (`#7C839C`)
- Accent: “chop” neon (`#6CFF7A`) for primary actions + selection outlines
- Danger: “shred” red (`#FF3B4E`) for destructive actions
- Borders/dividers: subtle stroke (`rgba(255,255,255,0.08)`)
- Radius: 12 for cards/panels; 999 for pills
- Spacing: 4/8/12/16/24/32 scale
- Typography: system font; sizes 12/14/16/20/24; bold for headings

### Interaction states
- Buttons: default (accent fill), pressed (accent darken + 0.95 scale), disabled (opacity 0.4)
- Tool items: default (surface), selected (accent outline + glow shadow)
- Sliders: track (muted), active track (accent), thumb (surface + border)

### Layout approach (desktop-first with responsive fallbacks)
- Primary layout uses a 3-column Grid-like arrangement:
  - Left: Tool Dock
  - Center: Canvas Stage
  - Right: Properties Panel
- On narrow screens:
  - Tool Dock becomes a bottom dock (horizontal)
  - Properties becomes a bottom sheet or modal panel

## Page: Home
### Meta Information
- Title: Faceshot Chopshop
- Description: Launch the editor shell and validate the theme.
- Open Graph: title + dark preview image (optional placeholder)

### Page Structure
- Stacked sections with max-width container (centered) for tablet/web; full-width for phones.

### Sections & Components
1. Top Bar
   - App wordmark: “faceshot-chopshop”
   - Secondary action: “Settings”
2. Quick Start Card
   - Primary CTA: “New Session”
   - Helper text: “Starts with a sample asset (shell only)”
3. Recent Sessions List
   - Session rows: name, timestamp, “Open”
   - Empty state: “No sessions yet”

## Page: Editor (4-tier shell)
### Meta Information
- Title: Editor – Faceshot Chopshop
- Description: 4-tier editor UI shell with injected logic.

### Page Structure (4 tiers)
- Tier 1 (top): Global Header
- Tier 2 (top): Context Bar
- Tier 3 (center): Canvas Stage
- Tier 4 (bottom/side): Tools + Properties

### Sections & Components
1. Tier 1: Global Header (sticky)
   - Left: Back button
   - Center: Document title (truncates gracefully)
   - Right: Status pill (e.g., “Shell Mode”) + optional menu
2. Tier 2: Context Bar
   - Left group: Undo / Redo (disabled/enabled based on injected state)
   - Middle: Zoom indicator placeholder (e.g., “100%”)
   - Right group: Export button (placeholder)
3. Tier 3: Canvas Stage
   - Centered canvas container with aspect-fit placeholder image
   - Optional overlay layer placeholder for future strokes/handles
   - Gesture affordances: zoom/pan hints (non-blocking)
4. Tier 4A: Tool Dock
   - Vertical list (desktop/tablet): icon + label; selected highlight
   - Horizontal bottom dock (phone)
   - Default tools (shell-level): Crop, Rotate, Adjust, Draw, Text (names only)
5. Tier 4B: Properties Panel
   - Header: selected tool name
   - Controls: slider rows, toggles, segmented options (rendered based on tool)
   - States: loading shimmer (if `isBusy`), empty state (“Select a tool”)

## Page: Settings
### Meta Information
- Title: Settings – Faceshot Chopshop
- Description: Theme preview and shell toggles.

### Page Structure
- Two-column on desktop/tablet: Theme Preview (left), Toggles (right)
- Single-column stack on phones

### Sections & Components
1. Theme Preview
   - Color swatches (background/surface/accent/danger)
   - Component preview: buttons, tool item, slider row
2. Developer Toggles
   - Switches: show layout grid, force busy state, mock canUndo/canRedo
   - Note: “Toggles affect shell only”
