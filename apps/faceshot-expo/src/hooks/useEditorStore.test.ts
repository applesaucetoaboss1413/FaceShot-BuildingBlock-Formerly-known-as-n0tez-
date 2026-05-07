import { act } from 'react-test-renderer'

import { useEditorStore } from './useEditorStore'

describe('useEditorStore', () => {
  beforeEach(() => {
    useEditorStore.setState((s) => ({
      ...s,
      media: null,
      tool: { selectedTool: null, intensity: 50, presets: { aiFilters: null, adjust: null, text: null, overlays: null } },
      undoRedo: { canUndo: false, canRedo: false },
      history: {
        past: [],
        present: { intensity: 50, selectedTool: null, presets: { aiFilters: null, adjust: null, text: null, overlays: null } },
        future: [],
      },
    }))
  })

  it('selectTool updates selected tool and enables undo', () => {
    act(() => {
      useEditorStore.getState().actions.selectTool('adjust')
    })

    const s = useEditorStore.getState()
    expect(s.tool.selectedTool).toBe('adjust')
    expect(s.undoRedo.canUndo).toBe(true)
    expect(s.undoRedo.canRedo).toBe(false)
  })

  it('setIntensity clamps to 0..100', () => {
    act(() => {
      useEditorStore.getState().actions.setIntensity(999)
    })
    expect(useEditorStore.getState().tool.intensity).toBe(100)

    act(() => {
      useEditorStore.getState().actions.setIntensity(-10)
    })
    expect(useEditorStore.getState().tool.intensity).toBe(0)
  })

  it('undo and redo restore previous state', () => {
    act(() => {
      useEditorStore.getState().actions.selectTool('aiFilters')
      useEditorStore.getState().actions.setIntensity(10)
      useEditorStore.getState().actions.setIntensity(20)
    })

    act(() => {
      useEditorStore.getState().actions.undo()
    })
    expect(useEditorStore.getState().tool.intensity).toBe(10)

    act(() => {
      useEditorStore.getState().actions.redo()
    })
    expect(useEditorStore.getState().tool.intensity).toBe(20)
  })

  it('setPreset updates preset map', () => {
    act(() => {
      useEditorStore.getState().actions.selectTool('aiFilters')
      useEditorStore.getState().actions.setPreset('aiFilters', 'aiGlow')
    })
    expect(useEditorStore.getState().tool.presets.aiFilters).toBe('aiGlow')
  })
})

