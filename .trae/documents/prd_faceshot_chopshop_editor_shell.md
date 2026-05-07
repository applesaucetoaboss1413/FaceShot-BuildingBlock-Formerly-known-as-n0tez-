## 1. Product Overview
A React Native (TypeScript) Expo Router app that scaffolds a reusable 4-tier editor UI shell.
It is themed to “faceshot-chopshop” and uses Zustand to inject editor logic without coupling UI to implementation.

## 2. Core Features

### 2.1 Feature Module
This app consists of the following main pages:
1. **Home**: quick start entry, recent sessions list, theme preview.
2. **Editor**: 4-tier editor shell (header, context bar, canvas stage, tool + properties tier).
3. **Settings**: theme controls preview (read-only), developer toggles for shell testing.

### 2.2 Page Details
| Page Name | Module Name | Feature description |
|---|---|---|
| Home | Quick Start | Start a new editor session using a sample asset and open the Editor. |
| Home | Recent Sessions | List recent sessions (in-memory for now) and reopen them. |
| Home | Global Navigation | Navigate to Settings and enter the Editor from primary CTAs. |
| Editor | Tier 1: Global Header | Show app identity (“faceshot-chopshop”), back navigation, and a persistent status area (e.g., “Saved/Unsaved”). |
| Editor | Tier 2: Context Bar | Show current document name, Undo/Redo placeholders, and Export placeholder actions. |
| Editor | Tier 3: Canvas Stage | Display the main workspace area (image/canvas placeholder) with safe area handling and zoom/pan placeholders. |
| Editor | Tier 4: Tool Dock + Properties | Show a tool list (icons + labels) and a context-sensitive properties panel that changes by selected tool. |
| Editor | Injected Logic via Zustand | Bind UI interactions (select tool, adjust slider, undo/redo) to an injected logic interface, allowing a swap-in implementation later. |
| Settings | Theme Preview | Display the theme tokens (colors, typography) and component preview cards using the same tokens as the Editor. |
| Settings | Developer Toggles | Toggle shell-only behaviors (e.g., “show layout grid”, “mock loading states”) for QA of the scaffold. |

## 3. Core Process
You open the app on Home, start a new session, and land in the Editor.
In the Editor, you select a tool in the tool dock, adjust its properties in the properties panel, and use Undo/Redo placeholders to test the shell flow.
You can exit back to Home, or open Settings to verify the faceshot-chopshop theme styling consistency.

```mermaid
graph TD
  A["Home"] --> B["Editor"]
  A --> C["Settings"]
  C