import React, { useState } from 'react';
import {
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAppTheme } from '@/hooks/useAppTheme';

export interface AppTextFieldProps extends TextInputProps {
  label: string;
  error?: string;
  helperText?: string;
  leadingIcon?: keyof typeof Ionicons.glyphMap;
  isPassword?: boolean;
  testID?: string;
}

export function AppTextField({
  label,
  error,
  helperText,
  leadingIcon,
  isPassword = false,
  testID,
  ...inputProps
}: AppTextFieldProps) {
  const { colors, typography } = useAppTheme();
  const [isFocused, setIsFocused] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const borderColor = error
    ? colors.error
    : isFocused
      ? colors.secondary
      : colors.border;

  return (
    <View style={styles.fieldContainer}>
      <Text
        style={[
          typography.labelLarge,
          styles.label,
          { color: colors.onSurface },
        ]}
      >
        {label}
      </Text>

      <View
        style={[
          styles.inputWrapper,
          {
            backgroundColor: colors.inputBackground,
            borderColor,
            borderWidth: isFocused || error ? 1.5 : 1,
          },
        ]}
      >
        {leadingIcon ? (
          <Ionicons
            name={leadingIcon}
            size={20}
            color={error ? colors.error : isFocused ? colors.secondary : colors.onSurfaceVariant}
            style={styles.leadingIcon}
          />
        ) : null}

        <TextInput
          {...inputProps}
          secureTextEntry={isPassword && !showPassword}
          placeholderTextColor={colors.onSurfaceVariant}
          onFocus={(e) => {
            setIsFocused(true);
            inputProps.onFocus?.(e);
          }}
          onBlur={(e) => {
            setIsFocused(false);
            inputProps.onBlur?.(e);
          }}
          style={[
            typography.bodyLarge,
            styles.input,
            { color: colors.onSurface },
          ]}
          testID={testID}
          accessibilityLabel={label}
        />

        {isPassword ? (
          <Pressable
            onPress={() => setShowPassword((prev) => !prev)}
            hitSlop={10}
            style={styles.trailingButton}
            accessibilityRole="button"
            accessibilityLabel={
              showPassword ? 'Hide password' : 'Show password'
            }
          >
            <Ionicons
              name={showPassword ? 'eye-off-outline' : 'eye-outline'}
              size={20}
              color={colors.onSurfaceVariant}
            />
          </Pressable>
        ) : null}
      </View>

      {error ? (
        <Text
          style={[
            typography.bodyMedium,
            styles.errorText,
            { color: colors.error },
          ]}
        >
          {error}
        </Text>
      ) : helperText ? (
        <Text
          style={[
            typography.bodyMedium,
            styles.helperText,
            { color: colors.onSurfaceVariant },
          ]}
        >
          {helperText}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  fieldContainer: {
    marginBottom: 14,
  },
  label: {
    marginBottom: 6,
  },
  inputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 50,
    borderRadius: 12,
    paddingHorizontal: 14,
  },
  leadingIcon: {
    marginRight: 10,
  },
  input: {
    flex: 1,
    paddingVertical: 10,
  },
  trailingButton: {
    marginLeft: 8,
    padding: 4,
  },
  errorText: {
    marginTop: 4,
  },
  helperText: {
    marginTop: 4,
  },
});
