import type React from 'react'
import { useMemo } from 'react'
import { Dimensions, View } from 'react-native'
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withTiming,
} from 'react-native-reanimated'
import { Gesture, GestureDetector } from 'react-native-gesture-handler'
import { Image } from 'expo-image'
import { Video, ResizeMode } from 'expo-av'

import type { EditorMedia } from '@/src/hooks/useEditorStore'

export type EditorCanvasProps = {
  media: EditorMedia
}

export function EditorCanvas(props: EditorCanvasProps): React.ReactElement {
  const scale = useSharedValue(1)
  const translateX = useSharedValue(0)
  const translateY = useSharedValue(0)
  const start = useSharedValue({ x: 0, y: 0 })
  const startScale = useSharedValue(1)

  const gesture = useMemo(() => {
    const pan = Gesture.Pan()
      .onBegin(() => {
        start.value = { x: translateX.value, y: translateY.value }
      })
      .onUpdate((e) => {
        translateX.value = start.value.x + e.translationX
        translateY.value = start.value.y + e.translationY
      })

    const pinch = Gesture.Pinch()
      .onBegin(() => {
        startScale.value = scale.value
      })
      .onUpdate((e) => {
        const next = startScale.value * e.scale
        scale.value = next < 0.6 ? 0.6 : next > 6 ? 6 : next
      })

    const doubleTap = Gesture.Tap()
      .numberOfTaps(2)
      .onEnd(() => {
        scale.value = withTiming(1)
        translateX.value = withTiming(0)
        translateY.value = withTiming(0)
      })

    return Gesture.Simultaneous(pan, pinch, doubleTap)
  }, [scale, start, startScale, translateX, translateY])

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: translateX.value }, { translateY: translateY.value }, { scale: scale.value }],
  }))

  const { width, height } = Dimensions.get('window')
  const canvasHeight = Math.max(280, Math.floor(height * 0.58))

  return (
    <GestureDetector gesture={gesture}>
      <View
        className="w-full items-center justify-center"
        style={{ height: canvasHeight }}
      >
        <View className="w-full h-full items-center justify-center overflow-hidden rounded-2xl border border-fs-stroke bg-fs-bg">
          <Animated.View style={[animatedStyle]}>
            {props.media.kind === 'photo' ? (
              <Image
                source={{ uri: props.media.uri }}
                style={{ width: width * 0.9, height: canvasHeight * 0.9, borderRadius: 16 }}
                contentFit="contain"
              />
            ) : (
              <Video
                source={{ uri: props.media.uri }}
                style={{ width: width * 0.9, height: canvasHeight * 0.9, borderRadius: 16 }}
                resizeMode={ResizeMode.CONTAIN}
                shouldPlay
                isLooping
                useNativeControls={false}
              />
            )}
          </Animated.View>
        </View>
      </View>
    </GestureDetector>
  )
}

