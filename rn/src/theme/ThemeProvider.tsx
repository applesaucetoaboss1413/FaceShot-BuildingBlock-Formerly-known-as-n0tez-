import React, { createContext, useContext, useMemo } from 'react'

import { colors, type FaceshotColors } from './colors'

export type FaceshotTheme = {
  colors: FaceshotColors
}

const ThemeContext = createContext<FaceshotTheme | null>(null)

export function FaceshotThemeProvider(props: { children: React.ReactNode }): React.ReactElement {
  const value = useMemo<FaceshotTheme>(() => ({ colors }), [])
  return <ThemeContext.Provider value={value}>{props.children}</ThemeContext.Provider>
}

export function useFaceshotTheme(): FaceshotTheme {
  const ctx = useContext(ThemeContext)
  if (!ctx) {
    throw new Error('FaceshotThemeProvider is missing')
  }
  return ctx
}

