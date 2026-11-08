/**
 * Wachemo University Teacher Evaluation System
 * Academic Color Palette (White, Dark Navy, and Professional Academic Blue)
 */

export const WachemoPalette = {
  navy950: '#06101E',
  navy900: '#0B192C',
  navy800: '#102A43',
  navy700: '#1E3A5F',
  blue700: '#1D4ED8',
  blue600: '#2563EB',
  blue500: '#3B82F6',
  blue100: '#DBEAFE',
  blue50: '#EFF6FF',
  gold600: '#D97706',
  gold500: '#F59E0B',
  gold100: '#FEF3C7',
  emerald600: '#059669',
  emerald100: '#D1FAE5',
  crimson600: '#DC2626',
  crimson100: '#FEE2E2',
  white: '#FFFFFF',
  slate50: '#F8FAFC',
  slate100: '#F1F5F9',
  slate200: '#E2E8F0',
  slate300: '#CBD5E1',
  slate500: '#64748B',
  slate600: '#475569',
  slate700: '#334155',
  slate800: '#1E293B',
  slate900: '#0F172A',
} as const;

export interface ThemeColorScheme {
  primary: string;
  onPrimary: string;
  primaryContainer: string;
  onPrimaryContainer: string;
  secondary: string;
  onSecondary: string;
  accent: string;
  background: string;
  onBackground: string;
  surface: string;
  onSurface: string;
  surfaceElevated: string;
  surfaceVariant: string;
  onSurfaceVariant: string;
  headerBackground: string;
  headerForeground: string;
  border: string;
  inputBackground: string;
  success: string;
  successContainer: string;
  warning: string;
  warningContainer: string;
  error: string;
  errorContainer: string;
}

export const LightColorScheme: ThemeColorScheme = {
  primary: WachemoPalette.navy800,
  onPrimary: WachemoPalette.white,
  primaryContainer: WachemoPalette.blue50,
  onPrimaryContainer: WachemoPalette.navy900,
  secondary: WachemoPalette.blue700,
  onSecondary: WachemoPalette.white,
  accent: WachemoPalette.gold600,
  background: WachemoPalette.slate50,
  onBackground: WachemoPalette.navy900,
  surface: WachemoPalette.white,
  onSurface: WachemoPalette.navy900,
  surfaceElevated: WachemoPalette.white,
  surfaceVariant: WachemoPalette.slate100,
  onSurfaceVariant: WachemoPalette.slate600,
  headerBackground: WachemoPalette.navy900,
  headerForeground: WachemoPalette.white,
  border: WachemoPalette.slate200,
  inputBackground: WachemoPalette.white,
  success: WachemoPalette.emerald600,
  successContainer: WachemoPalette.emerald100,
  warning: WachemoPalette.gold600,
  warningContainer: WachemoPalette.gold100,
  error: WachemoPalette.crimson600,
  errorContainer: WachemoPalette.crimson100,
};

export const DarkColorScheme: ThemeColorScheme = {
  primary: WachemoPalette.blue500,
  onPrimary: WachemoPalette.white,
  primaryContainer: WachemoPalette.navy700,
  onPrimaryContainer: WachemoPalette.blue100,
  secondary: WachemoPalette.blue600,
  onSecondary: WachemoPalette.white,
  accent: WachemoPalette.gold500,
  background: WachemoPalette.navy950,
  onBackground: WachemoPalette.slate50,
  surface: WachemoPalette.navy900,
  onSurface: WachemoPalette.slate50,
  surfaceElevated: WachemoPalette.navy800,
  surfaceVariant: WachemoPalette.navy800,
  onSurfaceVariant: WachemoPalette.slate300,
  headerBackground: WachemoPalette.navy950,
  headerForeground: WachemoPalette.white,
  border: WachemoPalette.navy700,
  inputBackground: WachemoPalette.navy800,
  success: '#10B981',
  successContainer: '#064E3B',
  warning: WachemoPalette.gold500,
  warningContainer: '#78350F',
  error: '#F87171',
  errorContainer: '#7F1D1D',
};
