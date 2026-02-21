/** @type {import('tailwindcss').Config} */
module.exports = {
  presets: [require('nativewind/preset')],
  content: [
    './app/**/*.{ts,tsx}',
    './src/**/*.{ts,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        fs: {
          bg: '#0B0C10',
          bg2: '#000000',
          surface: '#11131A',
          surface2: '#171A24',
          stroke: 'rgba(255,255,255,0.08)',
          text: '#F4F6FF',
          text2: '#B7BDD4',
          muted: '#7C839C',
          danger: '#FF3B4E',
          neonBlue: '#4D7CFE',
          neonPurple: '#A855F7',
        },
      },
    },
  },
  plugins: [],
}

