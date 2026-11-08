import React, { useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import { AppScreen } from '@/components/common/AppScreen';
import { UniversityHeader } from '@/components/common/UniversityHeader';
import { AppTextField } from '@/components/forms/AppTextField';
import { SelectPickerField } from '@/components/forms/SelectPickerField';
import { AppButton } from '@/components/common/AppButton';
import { StatusBanner } from '@/components/common/StatusBanner';
import {
  STUDY_YEARS,
  WCU_DEPARTMENTS,
  WCU_FACULTIES,
} from '@/constants/UniversityData';
import { validateStudentRegistration } from '@/utils/validators';
import { formatPendingPhase2Message } from '@/services/auth/authService';
import {
  StudentRegistrationFormValues,
  ValidationErrors,
} from '@/types/models';
import { useAppTheme } from '@/hooks/useAppTheme';

/**
 * 3. Student Registration Screen (`src/app/(auth)/register.tsx`)
 * Collects verified student details, enforces WCU student ID format,
 * dynamically filters departments by selected faculty, and strictly locks role to 'student'.
 */
export default function StudentRegistrationScreen() {
  const router = useRouter();
  const { colors, typography } = useAppTheme();

  const [values, setValues] = useState<StudentRegistrationFormValues>({
    fullName: '',
    studentIdNumber: '',
    email: '',
    facultyId: '',
    departmentId: '',
    yearOfStudy: '',
    password: '',
    confirmPassword: '',
  });

  const [errors, setErrors] = useState<
    ValidationErrors<StudentRegistrationFormValues>
  >({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [bannerMessage, setBannerMessage] = useState<{
    kind: 'info' | 'success' | 'error';
    text: string;
  } | null>(null);

  const facultyOptions = useMemo(
    () =>
      WCU_FACULTIES.map((fac) => ({
        label: `${fac.name} (${fac.code})`,
        value: fac.id,
        subtitle: fac.description,
      })),
    []
  );

  const filteredDepartmentOptions = useMemo(
    () =>
      WCU_DEPARTMENTS.filter(
        (dep) => !values.facultyId || dep.facultyId === values.facultyId
      ).map((dep) => ({
        label: `${dep.name} (${dep.code})`,
        value: dep.id,
      })),
    [values.facultyId]
  );

  const updateField = <K extends keyof StudentRegistrationFormValues>(
    key: K,
    val: StudentRegistrationFormValues[K]
  ) => {
    setValues((prev) => ({
      ...prev,
      [key]: val,
      ...(key === 'facultyId' ? { departmentId: '' } : {}),
    }));
    if (errors[key]) {
      setErrors((prev) => ({ ...prev, [key]: undefined }));
    }
  };

  const handleRegister = () => {
    setBannerMessage(null);
    const validationErrors = validateStudentRegistration(values);
    setErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      setBannerMessage({
        kind: 'error',
        text: 'Please review and complete all required academic registration fields.',
      });
      return;
    }

    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      setBannerMessage({
        kind: 'success',
        text: formatPendingPhase2Message(
          `Student Registration (${values.studentIdNumber.trim().toUpperCase()})`
        ),
      });
    }, 400);
  };

  return (
    <AppScreen
      edges={['bottom', 'left', 'right']}
      contentContainerStyle={styles.content}
      testID="register_screen"
    >
      <UniversityHeader
        compact
        title="Student Account Registration"
        subtitle="Register with your official Wachemo University Student ID"
      />

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
        <Text
          style={[
            typography.titleMedium,
            styles.sectionHeading,
            { color: colors.onSurface },
          ]}
        >
          1. Personal & Institutional Identity
        </Text>

        <AppTextField
          label="Full Name"
          placeholder="e.g., Abebe Kebede"
          value={values.fullName}
          onChangeText={(t) => updateField('fullName', t)}
          autoCapitalize="words"
          leadingIcon="person-outline"
          error={errors.fullName}
          testID="register_full_name_input"
        />

        <AppTextField
          label="Wachemo University Student ID"
          placeholder="e.g., WCU/1420/16"
          value={values.studentIdNumber}
          onChangeText={(t) => updateField('studentIdNumber', t)}
          autoCapitalize="characters"
          autoCorrect={false}
          leadingIcon="card-outline"
          helperText="Format: WCU/XXXX/YY or WCU/UR/XXXX/YY"
          error={errors.studentIdNumber}
          testID="register_student_id_input"
        />

        <AppTextField
          label="Email Address"
          placeholder="student.name@wcu.edu.et"
          value={values.email}
          onChangeText={(t) => updateField('email', t)}
          keyboardType="email-address"
          autoCapitalize="none"
          autoCorrect={false}
          leadingIcon="mail-outline"
          error={errors.email}
          testID="register_email_input"
        />

        <Text
          style={[
            typography.titleMedium,
            styles.sectionHeading,
            { color: colors.onSurface, marginTop: 8 },
          ]}
        >
          2. Academic Placement
        </Text>

        <SelectPickerField
          label="College / Faculty"
          placeholder="Select your College / Faculty"
          selectedValue={values.facultyId}
          options={facultyOptions}
          onValueChange={(val) => updateField('facultyId', val)}
          leadingIcon="business-outline"
          error={errors.facultyId}
          testID="register_faculty_picker"
        />

        <SelectPickerField
          label="Academic Department"
          placeholder={
            values.facultyId
              ? 'Select your Department'
              : 'Select a College / Faculty first'
          }
          selectedValue={values.departmentId}
          options={filteredDepartmentOptions}
          onValueChange={(val) => updateField('departmentId', val)}
          disabled={!values.facultyId}
          leadingIcon="library-outline"
          error={errors.departmentId}
          testID="register_department_picker"
        />

        <SelectPickerField
          label="Year of Study"
          placeholder="Select current Year of Study"
          selectedValue={values.yearOfStudy}
          options={STUDY_YEARS}
          onValueChange={(val) => updateField('yearOfStudy', val)}
          leadingIcon="calendar-outline"
          error={errors.yearOfStudy}
          testID="register_year_picker"
        />

        <Text
          style={[
            typography.titleMedium,
            styles.sectionHeading,
            { color: colors.onSurface, marginTop: 8 },
          ]}
        >
          3. Account Security
        </Text>

        <AppTextField
          label="Password"
          placeholder="Minimum 8 characters (letters & numbers)"
          value={values.password}
          onChangeText={(t) => updateField('password', t)}
          isPassword
          autoCapitalize="none"
          leadingIcon="lock-closed-outline"
          error={errors.password}
          testID="register_password_input"
        />

        <AppTextField
          label="Confirm Password"
          placeholder="Re-enter your password"
          value={values.confirmPassword}
          onChangeText={(t) => updateField('confirmPassword', t)}
          isPassword
          autoCapitalize="none"
          leadingIcon="shield-checkmark-outline"
          error={errors.confirmPassword}
          testID="register_confirm_password_input"
        />

        <StatusBanner
          kind="info"
          message="Security Notice: Student registration creates a verified 'student' role profile only. Administrative roles cannot be assigned from client registration."
        />

        <AppButton
          title="Register Student Account"
          onPress={handleRegister}
          isLoading={isSubmitting}
          variant="primary"
          iconName="checkmark-circle-outline"
          style={{ marginTop: 14 }}
          testID="register_submit_button"
        />
      </View>

      <View style={styles.footerRow}>
        <Text
          style={[typography.bodyMedium, { color: colors.onSurfaceVariant }]}
        >
          Already have a registered student account?
        </Text>
        <AppButton
          title="Back to Student Sign In"
          onPress={() => router.replace('/(auth)/login')}
          variant="outline"
          iconName="log-in-outline"
          testID="register_back_to_login_button"
        />
      </View>
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  content: {
    gap: 16,
    paddingBottom: 32,
  },
  formCard: {
    borderRadius: 16,
    padding: 18,
    borderWidth: 1,
  },
  sectionHeading: {
    marginBottom: 12,
  },
  footerRow: {
    gap: 10,
    alignItems: 'center',
  },
});
