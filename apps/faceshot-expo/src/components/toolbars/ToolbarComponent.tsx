import type React from 'react'
import { ScrollView, Text, TouchableOpacity, View } from 'react-native'
import { Ionicons } from '@expo/vector-icons'

import type { ToolId } from '@/src/hooks/useEditorStore'

export type ToolbarItem = {
  id: ToolId
  label: string
  icon: keyof typeof Ionicons.glyphMap
}

export type ToolbarComponentProps = {
  items: readonly ToolbarItem[]
  selected: ToolId | null
  onSelect: (id: ToolId) => void
}

export function ToolbarComponent(props: ToolbarComponentProps): React.ReactElement {
  return (
    <View className="w-full">
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerClassName="px-3" contentContainerStyle={{ gap: 10 }}>
        {props.items.map((item) => {
          const isSelected = item.id === props.selected
          return (
            <TouchableOpacity
              key={item.id}
              activeOpacity={0.85}
              onPress={() => props.onSelect(item.id)}
              className={
                isSelected
                  ? 'items-center justify-center rounded-2xl bg-fs-surface border border-fs-neonBlue'
                  : 'items-center justify-center rounded-2xl bg-fs-surface/70 border border-fs-stroke'
              }
              style={{ width: 55, height: 84 }}
            >
              <Ionicons name={item.icon} size={22} color={isSelected ? '#4D7CFE' : '#B7BDD4'} />
              <Text className="mt-1 text-[11px] text-fs-text2" numberOfLines={1}>
                {item.label}
              </Text>
            </TouchableOpacity>
          )
        })}
      </ScrollView>
    </View>
  )
}
