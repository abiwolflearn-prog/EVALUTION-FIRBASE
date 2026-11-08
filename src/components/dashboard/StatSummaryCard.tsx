import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAppTheme } from '@/hooks/useAppTheme';

export interface StatSummaryCardProps {
  label: string;
  value: string | number;
  iconName: keyof typeof Ionicons.glyphMap;
  subtitle?: string;
}

export function StatSummaryCard({
  label,
  value,
  iconName,
  subtitle,
}: StatSummaryCardProps) {
  const { colors, typography } = useAppTheme();

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: colors.surface,
          borderColor: colors.border,
        },
      ]}
    >
      <View
        style={[
          styles.iconBox,
          { backgroundColor: colors.primaryContainer },
        ]}
      >
        <Ionicons
          name={iconName}
          size={20}
          color={colors.secondary}
        />
      </View>
      <Text
        style={[
          typography.headlineMedium,
          { color: colors.onSurface, marginTop: 8 },
        ]}
      >
        {value}
      </Text>
      <Text
        style={[
          typography.labelLarge,
          { color: colors.onSurface },
        ]}
      >
        {label}
      </Text>
      {subtitle ? (
        <Text
          style={[
            typography.bodyMedium,
            { color: colors.onSurfaceVariant, marginTop: 2 },
          ]}
        >
          {subtitle}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flex: 1,
    minWidth: 140,
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
  },
  iconBox: {
    width: 36,
    height: 36,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
