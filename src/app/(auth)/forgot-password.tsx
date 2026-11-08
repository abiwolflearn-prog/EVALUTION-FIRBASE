import React, { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import { AppScreen } from '@/components/common/AppScreen';
import { UniversityHeader } from '@/components/common/UniversityHeader';
import { AppTextField } from '@/components/forms/AppTextField';
import { AppButton } from '@/components/common/AppButton';
import { StatusBanner } from '@/components/common/StatusBanner';
import { validatePasswordResetEmail } from '@/utils/validators';
import { formatPendingPhase2Message } from '@/services/auth/authService';
import { useAppTheme } from '@/hooks/useAppTheme';

/**
 * 5. Forgot Password Screen (`src/app/(auth)/forgot-password.tsx`)
 */
export default function ForgotPasswordScreen() {
  const router = useRouter();
  const { colors, typography } = useAppTheme();

  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | undefined>();
  const [isSending, setIsSending] = useState(false);
  const [sentMessage, setSentMessage] = useState<string | null>(null);

  const handleResetRequest = () => {
    setSentMessage(null);
    const validationError = validatePasswordResetEmail(email);
    if (validationError) {
      setError(validationError);
      return;
    }

    setError(undefined);
    setIsSending(true);
    setTimeout(() => {
      setIsSending(false);
      setSentMessage(
        formatPendingPhase2Message(`Password reset request for ${email.trim()}`)
      );
    }, 350);
  };

  return (
    <AppScreen
      edges={['bottom', 'left', 'right']}
      contentContainerStyle={styles.content}
      testID="forgot_password_screen"
    >
      <UniversityHeader
        compact
        title="Password Recovery"
        subtitle="Receive a secure Firebase Authentication password reset link"
      />

      {sentMessage ? (
        <StatusBanner
          kind="success"
          title="Reset Request Validated"
          message={sentMessage}
          onDismiss={() => setSentMessage(null)}
        />
      ) : null}

      <View
        style={[
          styles.card,
          {
            backgroundColor: colors.surface,
            borderColor: colors.border,
          },
        ]}
      >
        <Text
          style={[
            typography.bodyLarge,
            { color: colors.onSurfaceVariant, marginBottom: 14 },
          ]}
        >
          Enter the email address linked to your Wachemo University Student or
          Administrator account.
        </Text>

        <AppTextField
          label="Registered Email Address"
          placeholder="student.name@wcu.edu.et"
          value={email}
          onChangeText={(text) => {
            setEmail(text);
            if (error) setError(undefined);
          }}
          keyboardType="email-address"
          autoCapitalize="none"
          autoCorrect={false}
          leadingIcon="mail-outline"
          error={error}
          testID="forgot_password_email_input"
        />

        <AppButton
          title="Send Password Reset Link"
          onPress={handleResetRequest}
          isLoading={isSending}
          variant="primary"
          iconName="send-outline"
          testID="forgot_password_submit_button"
        />
      </View>

      <AppButton
        title="Return to Sign In"
        onPress={() => router.back()}
        variant="outline"
        iconName="arrow-back-outline"
        testID="forgot_password_back_button"
      />
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  content: {
    gap: 16,
    paddingBottom: 28,
  },
  card: {
    borderRadius: 16,
    padding: 18,
    borderWidth: 1,
  },
});
