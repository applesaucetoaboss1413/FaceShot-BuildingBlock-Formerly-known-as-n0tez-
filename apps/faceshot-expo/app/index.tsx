import type React from 'react'
import { useCallback } from 'react'
import { SafeAreaView, Text, View } from 'react-native'
import { useRouter } from 'expo-router'
import { LinearGradient } from 'expo-linear-gradient'
import * as ImagePicker from 'expo-image-picker'

import { PrimaryButton } from '@/src/components/toolbars/PrimaryButton'
import { useEditorStore, type EditorMedia } from '@/src/hooks/useEditorStore'
import { logger } from '@/src/utils/logger'

export default function HomeScreen(): React.ReactElement {
  const router = useRouter()
  const setMedia = useEditorStore((s) => s.setMedia)

  const pick = useCallback(
    async (kind: EditorMedia['kind']): Promise<void> => {
      try {
        const perm = await ImagePicker.requestMediaLibraryPermissionsAsync()
        if (!perm.granted) return

        const result = await ImagePicker.launchImageLibraryAsync({
          mediaTypes: kind === 'photo' ? ImagePicker.MediaTypeOptions.Images : ImagePicker.MediaTypeOptions.Videos,
          quality: 1,
          allowsEditing: false,
        })

        if (result.canceled) return
        const asset = result.assets[0]
        if (!asset?.uri) return

        setMedia({ kind, uri: asset.uri })
        router.push('/editor')
      } catch (e) {
        const error = e instanceof Error ? e : new Error('Media pick failed')
        logger.error('pick failed', { error })
      }
    },
    [router, setMedia],
  )

  return (
    <SafeAreaView className="flex-1 bg-fs-bg">
      <LinearGradient colors={['#000000', '#0B0C10']} style={{ flex: 1 }}>
        <View className="flex-1 px-6 pt-10">
          <Text className="text-fs-text text-2xl font-bold">faceshot-chopshop</Text>
          <Text className="text-fs-text2 mt-2 text-sm">Launch the editor shell with your own media.</Text>

          <View className="mt-8" style={{ gap: 12 }}>
            <PrimaryButton label="Open Photo" onPress={() => void pick('photo')} />
            <PrimaryButton label="Open Video" onPress={() => void pick('video')} />
            <PrimaryButton label="Settings" onPress={() => router.push('/settings')} />
          </View>
        </View>
      </LinearGradient>
    </SafeAreaView>
  )
}

