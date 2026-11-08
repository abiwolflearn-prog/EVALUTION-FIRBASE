import React from 'react';
import { Pressable } from 'react-native';
import { Stack } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useAppTheme } from '@/hooks/useAppTheme';

export default function AuthStackLayout() {
  const { colors, isDark, toggleTheme } = useAppTheme();

  return (
    <Stack
      screenOptions={{
        headerStyle: {
          backgroundColor: colors.headerBackground,
        },
        headerTintColor: colors.headerForeground,
        headerTitleStyle: {
          fontWeight: '700',
        },
        contentStyle: {
          backgroundColor: colors.background,
        },
        headerRight: () => (
          <Pressable
            onPress={toggleTheme}
            hitSlop={10}
            style={{ paddingHorizontal: 8 }}
            testID="auth_header_theme_toggle"
            accessibilityRole="button"
            accessibilityLabel="Switch light or dark theme"
          >
            <Ionicons
              name={isDark ? 'sunny-outline' : 'moon-outline'}
              size={22}
              color={colors.headerForeground}
            />
          </Pressable>
        ),
      }}
    >
      <Stack.Screen
        name="welcome"
        options={{
          title: 'Wachemo University',
        }}
      />
      <Stack.Screen
        name="login"
        options={{
          title: 'Sign In',
        }}
      />
      <Stack.Screen
        name="register"
        options={{
          title: 'Student Registration',
        }}
      />
      <Stack.Screen
        name="forgot-password"
        options={{
          title: 'Reset Password',
        }}
      />
    </Stack>
  );
}
