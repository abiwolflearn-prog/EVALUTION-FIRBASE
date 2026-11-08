import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAppTheme } from '@/hooks/useAppTheme';
import { UNIVERSITY_INFO } from '@/constants/UniversityData';
import { WachemoPalette } from '@/constants/Colors';

export interface UniversityHeaderProps {
  title?: string;
  subtitle?: string;
  compact?: boolean;
}

export function UniversityHeader({
  title = UNIVERSITY_INFO.name,
  subtitle = UNIVERSITY_INFO.systemTitle,
  compact = false,
}: UniversityHeaderProps) {
  const { colors, typography } = useAppTheme();

  return (
    <View
      style={[
        styles.banner,
        compact && styles.bannerCompact,
        {
          backgroundColor: colors.headerBackground,
          borderColor: WachemoPalette.navy700,
        },
      ]}
    >
      <View style={styles.topRow}>
        <View style={styles.crestBadge}>
          <Ionicons name="school" size={compact ? 24 : 30} color={WachemoPalette.gold500} />
        </View>
        <View style={styles.badgePill}>
          <Text style={[typography.labelSmall, { color: WachemoPalette.gold500 }]}>
            {UNIVERSITY_INFO.shortName} • {UNIVERSITY_INFO.campusLocation.toUpperCase()}
          </Text>
        </View>
      </View>

      <Text
        style={[
          compact ? typography.titleLarge : typography.headlineMedium,
          { color: colors.headerForeground, marginTop: compact ? 8 : 12 },
        ]}
      >
        {title}
      </Text>
      <Text
        style={[
          typography.bodyMedium,
          { color: WachemoPalette.blue100, marginTop: 4 },
        ]}
      >
        {subtitle}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: {
    borderRadius: 18,
    padding: 20,
    borderWidth: 1,
  },
  bannerCompact: {
    padding: 16,
    borderRadius: 14,
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  crestBadge: {
    width: 48,
    height: 48,
    borderRadius: 14,
    backgroundColor: 'rgba(255,255,255,0.1)',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(245, 158, 11, 0.35)',
  },
  badgePill: {
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 999,
    backgroundColor: 'rgba(255,255,255,0.08)',
  },
});
