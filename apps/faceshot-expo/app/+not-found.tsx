import type React from 'react'
import { Link, Stack } from 'expo-router'
import { SafeAreaView, Text, View } from 'react-native'

export default function NotFoundScreen(): React.ReactElement {
  return (
    <SafeAreaView className="flex-1 bg-fs-bg">
      <Stack.Screen options={{ title: 'Not Found' }} />
      <View className="flex-1 items-center justify-center px-6" style={{ gap: 12 }}>
        <Text className="text-fs-text text-lg font-semibold">This screen doesn’t exist.</Text>
        <Link href="/" asChild>
          <Text className="text-fs-neonBlue text-base">Go to home</Text>
        </Link>
      </View>
    </SafeAreaView>
  )
}
