import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAppTheme } from '@/hooks/useAppTheme';

export type BannerKind = 'info' | 'success' | 'warning' | 'error';

export interface StatusBannerProps {
  kind: BannerKind;
  title?: string;
  message: string;
  onDismiss?: () => void;
  testID?: string;
}

export function StatusBanner({
  kind,
  title,
  message,
  onDismiss,
  testID = 'status_banner',
}: StatusBannerProps) {
  const { colors, typography } = useAppTheme();

  const palette = {
    info: {
      bg: colors.primaryContainer,
      fg: colors.onPrimaryContainer,
      border: colors.secondary,
      icon: 'information-circle-outline' as const,
    },
    success: {
      bg: colors.successContainer,
      fg: colors.success,
      border: colors.success,
      icon: 'checkmark-circle-outline' as const,
    },
    warning: {
      bg: colors.warningContainer,
      fg: colors.warning,
      border: colors.warning,
      icon: 'warning-outline' as const,
    },
    error: {
      bg: colors.errorContainer,
      fg: colors.error,
      border: colors.error,
      icon: 'alert-circle-outline' as const,
    },
  }[kind];

  return (
    <View
      style={[
        styles.container,
        {
          backgroundColor: palette.bg,
          borderColor: palette.border,
        },
      ]}
      testID={testID}
      accessibilityRole="alert"
    >
      <Ionicons
        name={palette.icon}
        size={20}
        color={palette.fg}
        style={styles.icon}
      />
      <View style={styles.textWrap}>
        {title ? (
          <Text
            style={[
              typography.labelLarge,
              { color: palette.fg, marginBottom: 2 },
            ]}
          >
            {title}
          </Text>
        ) : null}
        <Text style={[typography.bodyMedium, { color: palette.fg }]}>
          {message}
        </Text>
      </View>
      {onDismiss ? (
        <Pressable
          onPress={onDismiss}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="Dismiss alert"
        >
          <Ionicons name="close" size={18} color={palette.fg} />
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    borderWidth: 1,
    borderRadius: 12,
    padding: 14,
    gap: 10,
  },
  icon: {
    marginTop: 1,
  },
  textWrap: {
    flex: 1,
  },
});
