import { Pressable, type PressableProps, View } from 'react-native'
import type React from 'react'

export type IconButtonProps = {
  children: React.ReactNode
  onPress: PressableProps['onPress']
  disabled?: boolean
}

export function IconButton(props: IconButtonProps): React.ReactElement {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={props.disabled}
      onPress={props.onPress}
      className="active:opacity-80"
      style={({ pressed }) => ({ opacity: props.disabled ? 0.4 : pressed ? 0.75 : 1 })}
    >
      <View className="h-10 w-10 items-center justify-center rounded-full bg-fs-surface/70 border border-fs-stroke">
        {props.children}
      </View>
    </Pressable>
  )
}

