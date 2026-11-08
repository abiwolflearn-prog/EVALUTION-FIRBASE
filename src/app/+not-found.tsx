import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Stack, useRouter } from 'expo-router';
import { AppScreen } from '@/components/common/AppScreen';
import { AppButton } from '@/components/common/AppButton';
import { useAppTheme } from '@/hooks/useAppTheme';

export default function NotFoundScreen() {
  const { colors, typography } = useAppTheme();
  const router = useRouter();

  return (
    <>
      <Stack.Screen options={{ title: 'Route Not Found' }} />
      <AppScreen scrollable={false} contentContainerStyle={styles.container}>
        <View style={styles.box}>
          <Text style={[typography.headlineMedium, { color: colors.onSurface }]}>
            Screen Not Found
          </Text>
          <Text
            style={[
              typography.bodyLarge,
              { color: colors.onSurfaceVariant, textAlign: 'center' },
            ]}
          >
            The requested Wachemo University Evaluation Portal screen does not
            exist.
          </Text>
          <AppButton
            title="Return to Welcome Screen"
            onPress={() => router.replace('/(auth)/welcome')}
            variant="primary"
            iconName="home-outline"
            testID="return_home_button"
          />
        </View>
      </AppScreen>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  box: {
    alignItems: 'center',
    gap: 14,
    maxWidth: 320,
  },
});
