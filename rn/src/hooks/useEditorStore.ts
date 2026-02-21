import { create } from 'zustand'

import { logger } from '@/src/utils/logger'

export type EditorMedia =
  | { kind: 'photo'; uri: string }
  | { kind: 'video'; uri: string }

export type ToolId = 'aiFilters' | 'adjust' | 'text' | 'overlays'

export type EditorToolState = {
  selectedTool: ToolId | null
  intensity: number
}

export type UndoRedoState = {
  canUndo: boolean
  canRedo: boolean
}

export type EditorLogicResult =
  | { ok: true }
  | { ok: false; error: Error }

export type EditorLogic = {
  selectTool(tool: ToolId): EditorLogicResult
  setIntensity(value: number): EditorLogicResult
  undo(): EditorLogicResult
  redo(): EditorLogicResult
  export(): Promise<EditorLogicResult>
}

function isEditorLogic(value: unknown): value is EditorLogic {
  if (!value || typeof value !== 'object') return false
  return (
    typeof Reflect.get(value, 'selectTool') === 'function' &&
    typeof Reflect.get(value, 'setIntensity') === 'function' &&
    typeof Reflect.get(value, 'undo') === 'function' &&
    typeof Reflect.get(value, 'redo') === 'function' &&
    typeof Reflect.get(value, 'export') === 'function'
  )
}

type HistoryEntry = { intensity: number; selectedTool: ToolId | null }

function clamp01To100(value: number): number {
  if (Number.isNaN(value)) return 0
  if (value < 0) return 0
  if (value > 100) return 100
  return value
}

function createDefaultLogic(get: () => EditorStoreState, set: (fn: (s: EditorStoreState) => EditorStoreState) => void): EditorLogic {
  return {
    selectTool(tool: ToolId): EditorLogicResult {
      set((s) => {
        const next: HistoryEntry = { intensity: s.tool.intensity, selectedTool: tool }
        const past = [...s.history.past, { intensity: s.tool.intensity, selectedTool: s.tool.selectedTool }]
        return {
          ...s,
          tool: { ...s.tool, selectedTool: tool },
          history: { past, present: next, future: [] },
          undoRedo: { canUndo: past.length > 0, canRedo: false },
        }
      })
      return { ok: true }
    },
    setIntensity(value: number): EditorLogicResult {
      const intensity = clamp01To100(value)
      set((s) => {
        const past = [...s.history.past, { intensity: s.tool.intensity, selectedTool: s.tool.selectedTool }]
        const present: HistoryEntry = { intensity, selectedTool: s.tool.selectedTool }
        return {
          ...s,
          tool: { ...s.tool, intensity },
          history: { past, present, future: [] },
          undoRedo: { canUndo: past.length > 0, canRedo: false },
        }
      })
      return { ok: true }
    },
    undo(): EditorLogicResult {
      const state = get()
      const past = state.history.past
      if (past.length === 0) return { ok: true }
      const previous = past[past.length - 1]
      const newPast = past.slice(0, -1)
      const future = [{ intensity: state.tool.intensity, selectedTool: state.tool.selectedTool }, ...state.history.future]
      set((s) => ({
        ...s,
        tool: { selectedTool: previous.selectedTool, intensity: previous.intensity },
        history: { past: newPast, present: previous, future },
        undoRedo: { canUndo: newPast.length > 0, canRedo: future.length > 0 },
      }))
      return { ok: true }
    },
    redo(): EditorLogicResult {
      const state = get()
      const future = state.history.future
      if (future.length === 0) return { ok: true }
      const next = future[0]
      const remaining = future.slice(1)
      const past = [...state.history.past, { intensity: state.tool.intensity, selectedTool: state.tool.selectedTool }]
      set((s) => ({
        ...s,
        tool: { selectedTool: next.selectedTool, intensity: next.intensity },
        history: { past, present: next, future: remaining },
        undoRedo: { canUndo: past.length > 0, canRedo: remaining.length > 0 },
      }))
      return { ok: true }
    },
    async export(): Promise<EditorLogicResult> {
      const state = get()
      if (!state.media) {
        return { ok: false, error: new Error('No media selected') }
      }
      return { ok: true }
    },
  }
}

export type EditorStoreState = {
  media: EditorMedia | null
  setMedia(media: EditorMedia): void
  clearMedia(): void

  setLogic(logic: EditorLogic): void

  tool: EditorToolState
  undoRedo: UndoRedoState
  history: {
    past: HistoryEntry[]
    present: HistoryEntry
    future: HistoryEntry[]
  }

  logic: EditorLogic
  actions: {
    selectTool(tool: ToolId): void
    setIntensity(value: number): void
    undo(): void
    redo(): void
    export(): Promise<void>
  }
}

export const useEditorStore = create<EditorStoreState>((set, get) => {
  const logic = createDefaultLogic(get, (fn) => set((s) => fn(s)))

  return {
    media: null,
    setMedia(media: EditorMedia): void {
      set((s) => ({ ...s, media }))
    },
    clearMedia(): void {
      set((s) => ({ ...s, media: null }))
    },

    setLogic(next: EditorLogic): void {
      if (!isEditorLogic(next)) {
        logger.error('setLogic rejected: invalid logic adapter')
        return
      }
      set((s) => ({ ...s, logic: next }))
    },

    tool: { selectedTool: null, intensity: 50 },
    undoRedo: { canUndo: false, canRedo: false },
    history: { past: [], present: { intensity: 50, selectedTool: null }, future: [] },

    logic,
    actions: {
      selectTool(tool: ToolId): void {
        const res = get().logic.selectTool(tool)
        if (!res.ok) logger.error('selectTool failed', { error: res.error })
      },
      setIntensity(value: number): void {
        const res = get().logic.setIntensity(value)
        if (!res.ok) logger.error('setIntensity failed', { error: res.error })
      },
      undo(): void {
        const res = get().logic.undo()
        if (!res.ok) logger.error('undo failed', { error: res.error })
      },
      redo(): void {
        const res = get().logic.redo()
        if (!res.ok) logger.error('redo failed', { error: res.error })
      },
      async export(): Promise<void> {
        const res = await get().logic.export()
        if (!res.ok) logger.error('export failed', { error: res.error })
      },
    },
  }
})
