import type React from 'react'
import { useMemo } from 'react'
import { ScrollView, Text, TouchableOpacity, View } from 'react-native'
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

type ToolPreset = { id: string; label: string }

type ToolConfig = {
  id: ToolId
  label: string
  icon: keyof typeof Ionicons.glyphMap
  presets?: readonly ToolPreset[]
  sliderLabel?: string
}

const toolRegistry: readonly ToolConfig[] = [
  {
    id: 'aiFilters',
    label: 'AI Filters',
    icon: 'sparkles',
    sliderLabel: 'Intensity',
    presets: [
      { id: 'aiGlow', label: 'Glow' },
      { id: 'aiStudio', label: 'Studio' },
      { id: 'aiNoir', label: 'Noir' },
      { id: 'aiCyber', label: 'Cyber' },
    ],
  },
  {
    id: 'adjust',
    label: 'Adjust',
    icon: 'options',
    sliderLabel: 'Strength',
    presets: [
      { id: 'adjustAuto', label: 'Auto' },
      { id: 'adjustContrast', label: 'Contrast' },
      { id: 'adjustClarity', label: 'Clarity' },
      { id: 'adjustWarm', label: 'Warm' },
    ],
  },
  {
    id: 'text',
    label: 'Text',
    icon: 'text',
    sliderLabel: 'Size',
    presets: [
      { id: 'textBold', label: 'Bold' },
      { id: 'textMono', label: 'Mono' },
      { id: 'textOutline', label: 'Outline' },
      { id: 'textGlow', label: 'Glow' },
    ],
  },
  {
    id: 'overlays',
    label: 'Overlay',
    icon: 'layers',
    sliderLabel: 'Opacity',
    presets: [
      { id: 'overlayGrain', label: 'Grain' },
      { id: 'overlayDust', label: 'Dust' },
      { id: 'overlayLight', label: 'Light' },
      { id: 'overlayPrism', label: 'Prism' },
    ],
  },
]

export function LayoutShell(): React.ReactElement {
  const router = useRouter()
  const media = useEditorStore((s) => s.media)
  const tool = useEditorStore((s) => s.tool)
  const undoRedo = useEditorStore((s) => s.undoRedo)
  const actions = useEditorStore((s) => s.actions)

  const items = useMemo<readonly ToolbarItem[]>(
    () => toolRegistry.map((toolConfig) => ({ id: toolConfig.id, label: toolConfig.label, icon: toolConfig.icon })),
    [],
  )
  const activeTool = useMemo(() => toolRegistry.find((toolConfig) => toolConfig.id === tool.selectedTool) ?? null, [tool.selectedTool])
  const activePreset = tool.selectedTool ? tool.presets[tool.selectedTool] : null

  if (!media) {
    return (
      <View className="flex-1 bg-fs-bg items-center justify-center px-6">
        <View className="w-full max-w-[420px]">
          <BlurView intensity={22} tint="dark" style={{ borderRadius: 20, overflow: 'hidden' }}>
            <View className="border border-fs-stroke px-5 py-6" style={{ borderRadius: 20 }}>
              <View className="flex-row items-center justify-between">
                <Text className="text-fs-text text-lg font-semibold">Editor Shell</Text>
                <View className="px-3 py-1 rounded-full border border-fs-stroke">
                  <Text className="text-fs-text2 text-[11px]">No Media</Text>
                </View>
              </View>
              <Text className="text-fs-text2 text-sm mt-2">
                Load a photo or video to reveal tools, presets, and controls.
              </Text>
              <View className="mt-5 flex-row items-center" style={{ gap: 10 }}>
                <View className="w-2 h-2 rounded-full bg-fs-neonBlue" />
                <Text className="text-fs-text2 text-xs">AI filters, overlays, and text presets ready.</Text>
              </View>
              <View className="mt-2 flex-row items-center" style={{ gap: 10 }}>
                <View className="w-2 h-2 rounded-full bg-fs-text" />
                <Text className="text-fs-text2 text-xs">Select media from the home screen.</Text>
              </View>
            </View>
          </BlurView>
        </View>
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

  function onSelectPreset(presetId: string): void {
    if (!tool.selectedTool) return
    actions.setPreset(tool.selectedTool, presetId)
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

        <View className="mt-3">
          <BlurView intensity={20} tint="dark" style={{ borderRadius: 18, overflow: 'hidden' }}>
            <View className="px-4 py-3 border border-fs-stroke" style={{ borderRadius: 18 }}>
              <View className="flex-row items-center justify-between">
                <Text className="text-fs-text text-sm font-semibold">{activeTool ? activeTool.label : 'Inspector'}</Text>
                <Text className="text-fs-text2 text-xs">{activeTool ? 'Active' : 'Idle'}</Text>
              </View>
              {activeTool?.presets ? (
                <ScrollView
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  contentContainerClassName="mt-3"
                  contentContainerStyle={{ gap: 8 }}
                >
                  {activeTool.presets.map((preset) => {
                    const isSelected = preset.id === activePreset
                    return (
                      <TouchableOpacity
                        key={preset.id}
                        activeOpacity={0.85}
                        onPress={() => onSelectPreset(preset.id)}
                        className={isSelected ? 'px-3 py-2 rounded-full bg-fs-text text-black' : 'px-3 py-2 rounded-full border border-fs-stroke'}
                      >
                        <Text className={isSelected ? 'text-[12px] font-semibold' : 'text-[12px] text-fs-text2'}>{preset.label}</Text>
                      </TouchableOpacity>
                    )
                  })}
                </ScrollView>
              ) : (
                <View className="mt-3">
                  <Text className="text-fs-text2 text-xs">Pick a tool to reveal controls.</Text>
                </View>
              )}
            </View>
          </BlurView>
        </View>
      </View>

      <AdjustmentDrawer
        visible={tool.selectedTool !== null}
        label={activeTool?.sliderLabel ?? 'Intensity'}
        value={tool.intensity}
        onChange={(v) => actions.setIntensity(v)}
      />
    </View>
  )
}
