import React, { useEffect } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { AppScreen } from '@/components/common/AppScreen';
import { AppButton } from '@/components/common/AppButton';
import { UNIVERSITY_INFO } from '@/constants/UniversityData';
import { WachemoPalette } from '@/constants/Colors';
import { useAuth } from '@/hooks/useAuth';
import { useAppTheme } from '@/hooks/useAppTheme';

/**
 * 1. Splash & Entry Gate Screen (`src/app/index.tsx`)
 * Displays Wachemo University branding while checking authentication state,
 * then navigates to `/(auth)/welcome` or the role-appropriate dashboard.
 */
export default function SplashScreenRoute() {
  const router = useRouter();
  const { user, role, isLoading } = useAuth();
  const { typography } = useAppTheme();

  useEffect(() => {
    if (isLoading) return;

    const timer = setTimeout(() => {
      if (user && role === 'admin') {
        router.replace('/(admin)');
      } else if (user && role === 'student') {
        router.replace('/(student)');
      } else {
        router.replace('/(auth)/welcome');
      }
    }, 900);

    return () => clearTimeout(timer);
  }, [isLoading, user, role, router]);

  return (
    <AppScreen
      scrollable={false}
      style={{ backgroundColor: WachemoPalette.navy900 }}
      contentContainerStyle={styles.container}
      testID="splash_screen"
    >
      <View style={styles.centerContent}>
        <View style={styles.emblemOuter}>
          <View style={styles.emblemInner}>
            <Ionicons
              name="school"
              size={48}
              color={WachemoPalette.gold500}
            />
          </View>
        </View>

        <Text
          style={[
            typography.labelSmall,
            styles.campusTag,
            { color: WachemoPalette.gold500 },
          ]}
        >
          {UNIVERSITY_INFO.campusLocation.toUpperCase()}
        </Text>

        <Text
          style={[
            typography.displayLarge,
            styles.universityTitle,
            { color: WachemoPalette.white },
          ]}
        >
          {UNIVERSITY_INFO.name}
        </Text>

        <Text
          style={[
            typography.bodyLarge,
            styles.subtitle,
            { color: WachemoPalette.blue100 },
          ]}
        >
          {UNIVERSITY_INFO.systemTitle}
        </Text>

        <View style={styles.loaderBox}>
          <ActivityIndicator size="large" color={WachemoPalette.blue500} />
          <Text
            style={[
              typography.bodyMedium,
              { color: WachemoPalette.slate300, marginTop: 10 },
            ]}
          >
            Initializing Academic Quality Portal...
          </Text>
        </View>
      </View>

      <View style={styles.footer}>
        <AppButton
          title="Continue to Welcome Screen"
          onPress={() => router.replace('/(auth)/welcome')}
          variant="primary"
          iconName="arrow-forward"
          testID="splash_continue_button"
        />
        <Text
          style={[
            typography.labelSmall,
            styles.mottoText,
            { color: WachemoPalette.slate300 },
          ]}
        >
          “{UNIVERSITY_INFO.motto}”
        </Text>
      </View>
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-between',
    padding: 24,
  },
  centerContent: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emblemOuter: {
    width: 112,
    height: 112,
    borderRadius: 32,
    backgroundColor: 'rgba(37, 99, 235, 0.18)',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
    borderColor: 'rgba(245, 158, 11, 0.45)',
    marginBottom: 20,
  },
  emblemInner: {
    width: 84,
    height: 84,
    borderRadius: 24,
    backgroundColor: WachemoPalette.navy800,
    alignItems: 'center',
    justifyContent: 'center',
  },
  campusTag: {
    marginBottom: 6,
  },
  universityTitle: {
    textAlign: 'center',
  },
  subtitle: {
    textAlign: 'center',
    marginTop: 8,
    maxWidth: 320,
  },
  loaderBox: {
    marginTop: 32,
    alignItems: 'center',
  },
  footer: {
    gap: 12,
  },
  mottoText: {
    textAlign: 'center',
  },
});
