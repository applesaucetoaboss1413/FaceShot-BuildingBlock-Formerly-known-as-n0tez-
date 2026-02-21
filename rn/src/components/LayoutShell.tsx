import type React from 'react'
import { useMemo } from 'react'
import { Text, View } from 'react-native'
import { BlurView } from 'expo-blur'
import { useRouter } from 'expo-router'
import { Ionicons } from '@expo/vector-icons'
import * as Sharing from 'expo-sharing'

import { useEditorStore, type ToolId } from '@/src/hooks/useEditorStore'
import { IconButton } from '@/src/components/toolbars/IconButton'
import { ToolbarComponent, type ToolbarItem } from '@/src/components/toolbars/ToolbarComponent'
import { AdjustmentDrawer } from '@/src/components/toolbars/AdjustmentDrawer'
import { EditorCanvas } from '@/src/components/canvas/EditorCanvas'
import { logger } from '@/src/utils/logger'

export function LayoutShell(): React.ReactElement {
  const router = useRouter()
  const media = useEditorStore((s) => s.media)
  const tool = useEditorStore((s) => s.tool)
  const undoRedo = useEditorStore((s) => s.undoRedo)
  const actions = useEditorStore((s) => s.actions)

  const items = useMemo<readonly ToolbarItem[]>(
    () => [
      { id: 'aiFilters', label: 'AI', icon: 'sparkles' },
      { id: 'adjust', label: 'Adjust', icon: 'options' },
      { id: 'text', label: 'Text', icon: 'text' },
      { id: 'overlays', label: 'Overlay', icon: 'layers' },
    ],
    [],
  )

  if (!media) {
    return (
      <View className="flex-1 bg-fs-bg items-center justify-center px-6">
        <Text className="text-fs-text text-base text-center">Select a photo or video to start editing.</Text>
      </View>
    )
  }

  const mediaUri = media.uri

  async function onExport(): Promise<void> {
    await actions.export()
    try {
      const canShare = await Sharing.isAvailableAsync()
      if (!canShare) return
      await Sharing.shareAsync(mediaUri)
    } catch (e) {
      const error = e instanceof Error ? e : new Error('Export failed')
      logger.error('shareAsync failed', { error })
    }
  }

  function onSelectTool(id: ToolId): void {
    actions.selectTool(id)
  }

  return (
    <View className="flex-1 bg-fs-bg">
      <View className="pt-3 px-3">
        <BlurView intensity={24} tint="dark" style={{ borderRadius: 18, overflow: 'hidden' }}>
          <View className="flex-row items-center justify-between px-3 py-2 border border-fs-stroke" style={{ borderRadius: 18 }}>
            <View className="flex-row items-center" style={{ gap: 10 }}>
              <IconButton onPress={() => router.back()}>
                <Ionicons name="chevron-back" size={18} color="#F4F6FF" />
              </IconButton>
              <Text className="text-fs-text font-semibold">Editor</Text>
            </View>
            <View className="flex-row items-center" style={{ gap: 10 }}>
              <IconButton disabled={!undoRedo.canUndo} onPress={() => actions.undo()}>
                <Ionicons name="arrow-undo" size={18} color="#F4F6FF" />
              </IconButton>
              <IconButton disabled={!undoRedo.canRedo} onPress={() => actions.redo()}>
                <Ionicons name="arrow-redo" size={18} color="#F4F6FF" />
              </IconButton>
              <IconButton onPress={() => void onExport()}>
                <Ionicons name="share" size={18} color="#F4F6FF" />
              </IconButton>
            </View>
          </View>
        </BlurView>
      </View>

      <View className="flex-1 px-3 pt-3">
        <EditorCanvas media={media} />

        <View className="mt-3">
          <BlurView intensity={22} tint="dark" style={{ borderRadius: 18, overflow: 'hidden' }}>
            <View className="py-3 border border-fs-stroke" style={{ borderRadius: 18 }}>
              <ToolbarComponent items={items} selected={tool.selectedTool} onSelect={onSelectTool} />
            </View>
          </BlurView>
        </View>
      </View>

      <AdjustmentDrawer
        visible={tool.selectedTool !== null}
        label="Intensity"
        value={tool.intensity}
        onChange={(v) => actions.setIntensity(v)}
      />
    </View>
  )
}
