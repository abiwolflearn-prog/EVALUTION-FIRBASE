import React from 'react';
import { StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { AppScreen } from '@/components/common/AppScreen';
import { UniversityHeader } from '@/components/common/UniversityHeader';
import { StatusBanner } from '@/components/common/StatusBanner';
import { AppButton } from '@/components/common/AppButton';

export default function StudentDashboardShellScreen() {
  const router = useRouter();

  return (
    <AppScreen
      edges={['bottom', 'left', 'right']}
      contentContainerStyle={styles.content}
      testID="student_dashboard_shell"
    >
      <UniversityHeader
        compact
        title="Student Portal"
        subtitle="Wachemo University Teacher Evaluation System"
      />
      <StatusBanner
        kind="info"
        title="Authentication Required (Phase 2)"
        message="Live Firestore student dashboard statistics, teacher directories, and evaluation forms are unlocked after Firebase Authentication in Phase 2–4."
      />
      <AppButton
        title="Return to Welcome Screen"
        onPress={() => router.replace('/(auth)/welcome')}
        variant="outline"
        iconName="arrow-back-outline"
      />
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  content: {
    gap: 16,
  },
});
