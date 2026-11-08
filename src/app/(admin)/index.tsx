import React from 'react';
import { StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { AppScreen } from '@/components/common/AppScreen';
import { UniversityHeader } from '@/components/common/UniversityHeader';
import { StatusBanner } from '@/components/common/StatusBanner';
import { AppButton } from '@/components/common/AppButton';

export default function AdminDashboardShellScreen() {
  const router = useRouter();

  return (
    <AppScreen
      edges={['bottom', 'left', 'right']}
      contentContainerStyle={styles.content}
      testID="admin_dashboard_shell"
    >
      <UniversityHeader
        compact
        title="Administrator Portal"
        subtitle="Restricted Academic Quality Assurance Console"
      />
      <StatusBanner
        kind="warning"
        title="Administrator Authorization Required"
        message="Access to student, teacher, faculty, department, course, and evaluation analytics requires a verified Administrator session via Firebase Authentication & Cloud Firestore (Phase 2–5)."
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
