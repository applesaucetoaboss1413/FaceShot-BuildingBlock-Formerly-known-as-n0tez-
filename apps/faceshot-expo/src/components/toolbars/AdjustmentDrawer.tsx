import type React from 'react'
import { Text, View } from 'react-native'
import Slider from '@react-native-community/slider'
import { BlurView } from 'expo-blur'

export type AdjustmentDrawerProps = {
  visible: boolean
  label: string
  value: number
  onChange: (value: number) => void
}

export function AdjustmentDrawer(props: AdjustmentDrawerProps): React.ReactElement | null {
  if (!props.visible) return null

  return (
    <View className="w-full px-3 pb-3">
      <BlurView intensity={24} tint="dark" style={{ borderRadius: 16, overflow: 'hidden' }}>
        <View className="px-4 py-3 border border-fs-stroke" style={{ borderRadius: 16 }}>
          <View className="flex-row items-center justify-between">
            <Text className="text-fs-text text-sm font-semibold">{props.label}</Text>
            <Text className="text-fs-text2 text-sm">{Math.round(props.value)}</Text>
          </View>
          <View className="mt-2">
            <Slider
              minimumValue={0}
              maximumValue={100}
              value={props.value}
              minimumTrackTintColor="#4D7CFE"
              maximumTrackTintColor="rgba(255,255,255,0.18)"
              thumbTintColor="#F4F6FF"
              onValueChange={(v) => props.onChange(typeof v === 'number' ? v : Array.isArray(v) ? v[0] ?? 0 : 0)}
            />
          </View>
        </View>
      </BlurView>
    </View>
  )
}

