import type React from 'react'
import { Pressable, Text, View } from 'react-native'
import { LinearGradient } from 'expo-linear-gradient'

export type PrimaryButtonProps = {
  label: string
  onPress: () => void
  disabled?: boolean
}

export function PrimaryButton(props: PrimaryButtonProps): React.ReactElement {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={props.disabled}
      onPress={props.onPress}
      style={({ pressed }) => ({ opacity: props.disabled ? 0.4 : pressed ? 0.85 : 1, transform: [{ scale: pressed ? 0.99 : 1 }] })}
      className="w-full"
    >
      <LinearGradient
        colors={['#4D7CFE', '#A855F7']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={{ borderRadius: 14 }}
      >
        <View className="px-4 py-3">
          <Text className="text-fs-text text-base font-semibold text-center">{props.label}</Text>
        </View>
      </LinearGradient>
    </Pressable>
  )
}

