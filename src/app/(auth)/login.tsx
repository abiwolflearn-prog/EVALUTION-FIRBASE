import React, { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { AppScreen } from '@/components/common/AppScreen';
import { UniversityHeader } from '@/components/common/UniversityHeader';
import { AppTextField } from '@/components/forms/AppTextField';
import { AppButton } from '@/components/common/AppButton';
import { StatusBanner } from '@/components/common/StatusBanner';
import { validateLoginForm } from '@/utils/validators';
import { formatPendingPhase2Message } from '@/services/auth/authService';
import { LoginFormValues, UserRole, ValidationErrors } from '@/types/models';
import { useAppTheme } from '@/hooks/useAppTheme';

/**
 * 4. Login Screen (`src/app/(auth)/login.tsx`)
 * Supports Student and Administrator sign-in flows with strict form validation
 * and role-based security notices.
 */
export default function LoginScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ role?: string }>();
  const { colors, typography } = useAppTheme();

  const initialRole: UserRole = params.role === 'admin' ? 'admin' : 'student';

  const [values, setValues] = useState<LoginFormValues>({
    email: '',
    password: '',
    intendedRole: initialRole,
  });
  const [errors, setErrors] = useState<ValidationErrors<LoginFormValues>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [bannerMessage, setBannerMessage] = useState<{
    kind: 'info' | 'success' | 'error';
    text: string;
  } | null>(null);

  const handleRoleSwitch = (role: UserRole) => {
    setValues((prev) => ({ ...prev, intendedRole: role }));
    setErrors({});
    setBannerMessage(null);
  };

  const handleSubmit = () => {
    setBannerMessage(null);
    const validationErrors = validateLoginForm(values);
    setErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      setBannerMessage({
        kind: 'error',
        text: 'Please correct the highlighted fields before signing in.',
      });
      return;
    }

    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      setBannerMessage({
        kind: 'info',
        text: formatPendingPhase2Message(
          values.intendedRole === 'admin'
            ? 'Administrator Login'
            : 'Student Login'
        ),
      });
    }, 350);
  };

  return (
    <AppScreen
      edges={['bottom', 'left', 'right']}
      contentContainerStyle={styles.content}
      testID="login_screen"
    >
      <UniversityHeader
        compact
        title={
          values.intendedRole === 'admin'
            ? 'Administrator Portal Sign In'
            : 'Student Portal Sign In'
        }
        subtitle="Sign in with your Wachemo University credentials"
      />

      <View
        style={[
          styles.roleSegmentContainer,
          {
            backgroundColor: colors.surfaceVariant,
            borderColor: colors.border,
          },
        ]}
      >
        <Pressable
          onPress={() => handleRoleSwitch('student')}
          style={[
            styles.roleSegmentTab,
            values.intendedRole === 'student' && {
              backgroundColor: colors.secondary,
            },
          ]}
          testID="login_role_student_tab"
          accessibilityRole="tab"
          accessibilityState={{ selected: values.intendedRole === 'student' }}
        >
          <Ionicons
            name="school-outline"
            size={16}
            color={
              values.intendedRole === 'student'
                ? colors.onSecondary
                : colors.onSurfaceVariant
            }
          />
          <Text
            style={[
              typography.labelLarge,
              {
                color:
                  values.intendedRole === 'student'
                    ? colors.onSecondary
                    : colors.onSurfaceVariant,
              },
            ]}
          >
            Student
          </Text>
        </Pressable>

        <Pressable
          onPress={() => handleRoleSwitch('admin')}
          style={[
            styles.roleSegmentTab,
            values.intendedRole === 'admin' && {
              backgroundColor: colors.primary,
            },
          ]}
          testID="login_role_admin_tab"
          accessibilityRole="tab"
          accessibilityState={{ selected: values.intendedRole === 'admin' }}
        >
          <Ionicons
            name="shield-checkmark-outline"
            size={16}
            color={
              values.intendedRole === 'admin'
                ? colors.onPrimary
                : colors.onSurfaceVariant
            }
          />
          <Text
            style={[
              typography.labelLarge,
              {
                color:
                  values.intendedRole === 'admin'
                    ? colors.onPrimary
                    : colors.onSurfaceVariant,
              },
            ]}
          >
            Administrator
          </Text>
        </Pressable>
      </View>

      {values.intendedRole === 'admin' ? (
        <StatusBanner
          kind="warning"
          title="Restricted Administrator Access"
          message="Administrator privileges cannot be self-registered. Accounts are provisioned by authorized Wachemo University ICT/Academic Quality officers and verified against Firestore role records."
        />
      ) : null}

      {bannerMessage ? (
        <StatusBanner
          kind={bannerMessage.kind}
          message={bannerMessage.text}
          onDismiss={() => setBannerMessage(null)}
        />
      ) : null}

      <View
        style={[
          styles.formCard,
          {
            backgroundColor: colors.surface,
            borderColor: colors.border,
          },
        ]}
      >
        <AppTextField
          label={
            values.intendedRole === 'admin'
              ? 'Administrator Email'
              : 'Student Email Address'
          }
          placeholder={
            values.intendedRole === 'admin'
              ? 'admin.qa@wcu.edu.et'
              : 'student.name@wcu.edu.et'
          }
          value={values.email}
          onChangeText={(text) => {
            setValues((prev) => ({ ...prev, email: text }));
            if (errors.email) setErrors((prev) => ({ ...prev, email: undefined }));
          }}
          keyboardType="email-address"
          autoCapitalize="none"
          autoCorrect={false}
          leadingIcon="mail-outline"
          error={errors.email}
          testID="login_email_input"
        />

        <AppTextField
          label="Password"
          placeholder="Enter your password"
          value={values.password}
          onChangeText={(text) => {
            setValues((prev) => ({ ...prev, password: text }));
            if (errors.password)
              setErrors((prev) => ({ ...prev, password: undefined }));
          }}
          isPassword
          autoCapitalize="none"
          leadingIcon="lock-closed-outline"
          error={errors.password}
          testID="login_password_input"
        />

        <View style={styles.forgotRow}>
          <Pressable
            onPress={() => router.push('/(auth)/forgot-password')}
            hitSlop={8}
            testID="login_forgot_password_link"
            accessibilityRole="link"
          >
            <Text
              style={[
                typography.labelLarge,
                { color: colors.secondary },
              ]}
            >
              Forgot Password?
            </Text>
          </Pressable>
        </View>

        <AppButton
          title={
            values.intendedRole === 'admin'
              ? 'Sign In as Administrator'
              : 'Sign In as Student'
          }
          onPress={handleSubmit}
          isLoading={isSubmitting}
          variant={values.intendedRole === 'admin' ? 'secondary' : 'primary'}
          iconName="log-in-outline"
          testID="login_submit_button"
        />
      </View>

      {values.intendedRole === 'student' ? (
        <View style={styles.footerPrompt}>
          <Text
            style={[
              typography.bodyMedium,
              { color: colors.onSurfaceVariant },
            ]}
          >
            New to the Wachemo University Evaluation Portal?
          </Text>
          <AppButton
            title="Create Student Account"
            onPress={() => router.push('/(auth)/register')}
            variant="outline"
            iconName="person-add-outline"
            testID="login_go_to_register_button"
          />
        </View>
      ) : null}
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  content: {
    gap: 16,
    paddingBottom: 28,
  },
  roleSegmentContainer: {
    flexDirection: 'row',
    borderRadius: 12,
    padding: 4,
    borderWidth: 1,
    gap: 6,
  },
  roleSegmentTab: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 10,
    borderRadius: 9,
    gap: 6,
  },
  formCard: {
    borderRadius: 16,
    padding: 18,
    borderWidth: 1,
  },
  forgotRow: {
    alignItems: 'flex-end',
    marginBottom: 16,
    marginTop: -4,
  },
  footerPrompt: {
    gap: 10,
    alignItems: 'center',
    marginTop: 4,
  },
});
