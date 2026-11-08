import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAppTheme } from '@/hooks/useAppTheme';
import { RATING_SCALE_LABELS } from '@/constants/UniversityData';
import { RatingValue } from '@/types/models';

export function RatingScaleLegend() {
  const { colors, typography } = useAppTheme();
  const ratings: RatingValue[] = [1, 2, 3, 4, 5];

  return (
    <View
      style={[
        styles.container,
        {
          backgroundColor: colors.surface,
          borderColor: colors.border,
        },
      ]}
    >
      <Text style={[typography.labelLarge, { color: colors.onSurface }]}>
        Official 5-Star Rating Scale
      </Text>
      <View style={styles.row}>
        {ratings.map((val) => (
          <View
            key={val}
            style={[
              styles.chip,
              { backgroundColor: colors.surfaceVariant },
            ]}
          >
            <Ionicons name="star" size={13} color={colors.accent} />
            <Text
              style={[
                typography.labelSmall,
                { color: colors.onSurface, marginLeft: 4 },
              ]}
            >
              {val} = {RATING_SCALE_LABELS[val]}
            </Text>
          </View>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    gap: 8,
  },
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 999,
  },
});
