import React from 'react';
import { StyleSheet, View, ViewStyle, StyleProp } from 'react-native';
import { SafeAreaView, Edge } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import { useAppTheme } from '@/hooks/useAppTheme';

export interface ScaffoldProps {
  children: React.ReactNode;
  style?: StyleProp<ViewStyle>;
  contentContainerStyle?: StyleProp<ViewStyle>;
  edges?: readonly Edge[];
  testID?: string;
}

/**
 * Reusable React Native equivalent of Jetpack Compose's `Scaffold(modifier = Modifier.fillMaxSize())`
 * combined with `enableEdgeToEdge()`.
 */
export function Scaffold({
  children,
  style,
  contentContainerStyle,
  edges = ['top', 'bottom', 'left', 'right'],
  testID = 'app_scaffold',
}: ScaffoldProps) {
  const { colors, isDark } = useAppTheme();

  return (
    <SafeAreaView
      edges={edges}
      style={[styles.safeArea, { backgroundColor: colors.background }, style]}
      testID={testID}
    >
      <StatusBar
        style={isDark ? 'light' : 'dark'}
        backgroundColor="transparent"
        translucent
      />
      <View style={[styles.innerContent, contentContainerStyle]}>
        {children}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  innerContent: {
    flex: 1,
  },
});
