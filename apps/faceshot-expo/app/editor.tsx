import type React from 'react'
import { SafeAreaView } from 'react-native'

import { LayoutShell } from '@/src/components/LayoutShell'

export default function EditorScreen(): React.ReactElement {
  return (
    <SafeAreaView className="flex-1 bg-fs-bg">
      <LayoutShell />
    </SafeAreaView>
  )
}

