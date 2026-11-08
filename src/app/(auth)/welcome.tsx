import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { AppScreen } from '@/components/common/AppScreen';
import { UniversityHeader } from '@/components/common/UniversityHeader';
import { AppButton } from '@/components/common/AppButton';
import { RatingScaleLegend } from '@/components/evaluation/RatingScaleLegend';
import { EVALUATION_CATEGORIES } from '@/constants/UniversityData';
import { useAppTheme } from '@/hooks/useAppTheme';

interface FeatureHighlight {
  icon: keyof typeof Ionicons.glyphMap;
  title: string;
  description: string;
}

const HIGHLIGHTS: FeatureHighlight[] = [
  {
    icon: 'Analytics-outline' as keyof typeof Ionicons.glyphMap,
    title: '6 Standardized Evaluation Pillars',
    description:
      'Evaluate Teaching Quality, Communication, Punctuality, Course Management, Student Support, and Ethics.',
  },
  {
    icon: 'shield-checkmark-outline',
    title: 'Verified & Duplicate-Protected',
    description:
      'One verified evaluation per student, teacher, course, and academic semester enforced via Firestore.',
  },
  {
    icon: 'lock-closed-outline',
    title: 'Role-Based Access Control',
    description:
      'Dedicated Student and Administrator portals with strict database-level authorization rules.',
  },
];

/**
 * 2. Welcome Screen (`src/app/(auth)/welcome.tsx`)
 */
export default function WelcomeScreen() {
  const router = useRouter();
  const { colors, typography } = useAppTheme();

  return (
    <AppScreen
      edges={['bottom', 'left', 'right']}
      contentContainerStyle={styles.content}
      testID="welcome_screen"
    >
      <UniversityHeader />

      <View
        style={[
          styles.sectionCard,
          {
            backgroundColor: colors.surface,
            borderColor: colors.border,
          },
        ]}
      >
        <Text style={[typography.titleLarge, { color: colors.onSurface }]}>
          Academic Quality Assurance Portal
        </Text>
        <Text
          style={[
            typography.bodyMedium,
            { color: colors.onSurfaceVariant, marginTop: 4 },
          ]}
        >
          Empowering Wachemo University students and academic leadership with
          structured, transparent, and secure instructor evaluations across all
          colleges and departments.
        </Text>

        <View style={styles.highlightsList}>
          {HIGHLIGHTS.map((item, idx) => (
            <View key={item.title} style={styles.highlightRow}>
              <View
                style={[
                  styles.iconBadge,
                  { backgroundColor: colors.primaryContainer },
                ]}
              >
                <Ionicons
                  name={
                    idx === 0
                      ? 'ribbon-outline'
                      : idx === 1
                        ? 'shield-checkmark-outline'
                        : 'lock-closed-outline'
                  }
                  size={20}
                  color={colors.secondary}
                />
              </View>
              <View style={styles.highlightText}>
                <Text
                  style={[typography.titleMedium, { color: colors.onSurface }]}
                >
                  {item.title}
                </Text>
                <Text
                  style={[
                    typography.bodyMedium,
                    { color: colors.onSurfaceVariant, marginTop: 2 },
                  ]}
                >
                  {item.description}
                </Text>
              </View>
            </View>
          ))}
        </View>
      </View>

      <RatingScaleLegend />

      <View
        style={[
          styles.sectionCard,
          {
            backgroundColor: colors.surface,
            borderColor: colors.border,
          },
        ]}
      >
        <Text style={[typography.labelLarge, { color: colors.onSurface }]}>
          Evaluation Framework ({EVALUATION_CATEGORIES.length} Categories • 18 Criteria)
        </Text>
        <View style={styles.categoryGrid}>
          {EVALUATION_CATEGORIES.map((cat) => (
            <View
              key={cat.key}
              style={[
                styles.categoryBadge,
                {
                  backgroundColor: colors.surfaceVariant,
                  borderColor: colors.border,
                },
              ]}
            >
              <Text
                style={[
                  typography.labelSmall,
                  { color: colors.secondary },
                ]}
              >
                0{cat.number}
              </Text>
              <Text
                style={[
                  typography.bodyMedium,
                  {
                    color: colors.onSurface,
                    fontWeight: '600',
                    flex: 1,
                  },
                ]}
                numberOfLines={1}
              >
                {cat.title}
              </Text>
            </View>
          ))}
        </View>
      </View>

      <View style={styles.ctaGroup}>
        <AppButton
          title="Student Sign In"
          onPress={() =>
            router.push({
              pathname: '/(auth)/login',
              params: { role: 'student' },
            })
          }
          variant="primary"
          iconName="log-in-outline"
          testID="welcome_student_login_button"
        />

        <AppButton
          title="Register New Student Account"
          onPress={() => router.push('/(auth)/register')}
          variant="secondary"
          iconName="person-add-outline"
          testID="welcome_student_register_button"
        />

        <AppButton
          title="Administrator Sign In"
          onPress={() =>
            router.push({
              pathname: '/(auth)/login',
              params: { role: 'admin' },
            })
          }
          variant="outline"
          iconName="key-outline"
          testID="welcome_admin_login_button"
        />
      </View>
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  content: {
    gap: 16,
    paddingBottom: 28,
  },
  sectionCard: {
    borderRadius: 16,
    padding: 18,
    borderWidth: 1,
    gap: 10,
  },
  highlightsList: {
    marginTop: 8,
    gap: 14,
  },
  highlightRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
  },
  iconBadge: {
    width: 40,
    height: 40,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 2,
  },
  highlightText: {
    flex: 1,
  },
  categoryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 4,
  },
  categoryBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 10,
    paddingVertical: 8,
    borderRadius: 10,
    borderWidth: 1,
    width: '48%',
  },
  ctaGroup: {
    gap: 10,
    marginTop: 4,
  },
});
