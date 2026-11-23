/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: { sans: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif'] },
      colors: { navy: { 950: '#080f24', 900: '#0b1530', 800: '#111f45', 700: '#1a2b5c', 600: '#27407f' } },
    },
  },
  plugins: [],
};
