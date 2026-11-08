import { Platform, TextStyle } from 'react-native';

const defaultFontFamily = Platform.select({
  android: 'sans-serif',
  ios: 'System',
  default: 'sans-serif',
});

export const Typography: Record<
  | 'displayLarge'
  | 'headlineMedium'
  | 'titleLarge'
  | 'titleMedium'
  | 'bodyLarge'
  | 'bodyMedium'
  | 'labelLarge'
  | 'labelSmall',
  TextStyle
> = {
  displayLarge: {
    fontFamily: defaultFontFamily,
    fontWeight: '700',
    fontSize: 28,
    lineHeight: 36,
    letterSpacing: -0.3,
  },
  headlineMedium: {
    fontFamily: defaultFontFamily,
    fontWeight: '700',
    fontSize: 22,
    lineHeight: 28,
    letterSpacing: -0.2,
  },
  titleLarge: {
    fontFamily: defaultFontFamily,
    fontWeight: '700',
    fontSize: 18,
    lineHeight: 24,
    letterSpacing: 0,
  },
  titleMedium: {
    fontFamily: defaultFontFamily,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 22,
    letterSpacing: 0.1,
  },
  bodyLarge: {
    fontFamily: defaultFontFamily,
    fontWeight: '400',
    fontSize: 15,
    lineHeight: 22,
    letterSpacing: 0.15,
  },
  bodyMedium: {
    fontFamily: defaultFontFamily,
    fontWeight: '400',
    fontSize: 13,
    lineHeight: 19,
    letterSpacing: 0.2,
  },
  labelLarge: {
    fontFamily: defaultFontFamily,
    fontWeight: '600',
    fontSize: 14,
    lineHeight: 20,
    letterSpacing: 0.3,
  },
  labelSmall: {
    fontFamily: defaultFontFamily,
    fontWeight: '600',
    fontSize: 11,
    lineHeight: 16,
    letterSpacing: 0.6,
  },
};
