import React from 'react';
import { Text, TextProps } from 'react-native';
import { useAppTheme } from '@/hooks/useAppTheme';
import { Typography } from '@/constants/Typography';

export interface ThemedTextProps extends TextProps {
  variant?: keyof typeof Typography;
  colorKey?:
    | 'onBackground'
    | 'onSurface'
    | 'onSurfaceVariant'
    | 'primary'
    | 'onPrimary'
    | 'onPrimaryContainer';
}

export function ThemedText({
  style,
  variant = 'bodyLarge',
  colorKey = 'onSurface',
  ...rest
}: ThemedTextProps) {
  const { colors, typography } = useAppTheme();

  return (
    <Text
      style={[
        typography[variant],
        { color: colors[colorKey] },
        style,
      ]}
      {...rest}
    />
  );
}
