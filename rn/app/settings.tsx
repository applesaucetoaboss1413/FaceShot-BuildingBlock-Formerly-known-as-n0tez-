import type React from 'react'
import { SafeAreaView, Text, View } from 'react-native'
import { useRouter } from 'expo-router'
import { LinearGradient } from 'expo-linear-gradient'
import { Ionicons } from '@expo/vector-icons'

import { IconButton } from '@/src/components/toolbars/IconButton'
import { useFaceshotTheme } from '@/src/theme/ThemeProvider'

export default function SettingsScreen(): React.ReactElement {
  const router = useRouter()
  const theme = useFaceshotTheme()

  return (
    <SafeAreaView className="flex-1 bg-fs-bg">
      <View className="px-3 pt-3">
        <View className="flex-row items-center" style={{ gap: 10 }}>
          <IconButton onPress={() => router.back()}>
            <Ionicons name="chevron-back" size={18} color="#F4F6FF" />
          </IconButton>
          <Text className="text-fs-text text-base font-semibold">Settings</Text>
        </View>
      </View>

      <View className="flex-1 px-6 pt-6" style={{ gap: 16 }}>
        <Text className="text-fs-text2 text-sm">Theme Tokens</Text>

        <View className="rounded-2xl border border-fs-stroke bg-fs-surface p-4" style={{ gap: 10 }}>
          <Text className="text-fs-text font-semibold">Background</Text>
          <View className="flex-row" style={{ gap: 10 }}>
            <View style={{ width: 44, height: 44, borderRadius: 12, backgroundColor: theme.colors.bg }} />
            <View style={{ width: 44, height: 44, borderRadius: 12, backgroundColor: theme.colors.surface }} />
          </View>
        </View>

        <View className="rounded-2xl border border-fs-stroke bg-fs-surface p-4" style={{ gap: 10 }}>
          <Text className="text-fs-text font-semibold">Primary Gradient</Text>
          <LinearGradient
            colors={[theme.colors.neonBlue, theme.colors.neonPurple]}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={{ height: 44, borderRadius: 14 }}
          />
        </View>
      </View>
    </SafeAreaView>
  )
}

