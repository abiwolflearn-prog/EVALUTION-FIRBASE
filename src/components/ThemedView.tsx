import React from 'react';
import { View, ViewProps } from 'react-native';
import { useAppTheme } from '@/hooks/useAppTheme';

export interface ThemedViewProps extends ViewProps {
  surface?: 'background' | 'surface' | 'surfaceVariant' | 'primaryContainer';
}

export function ThemedView({
  style,
  surface = 'background',
  ...rest
}: ThemedViewProps) {
  const { colors } = useAppTheme();

  return (
    <View
      style={[{ backgroundColor: colors[surface] }, style]}
      {...rest}
    />
  );
}
