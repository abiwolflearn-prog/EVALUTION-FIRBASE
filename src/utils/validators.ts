import {
  LoginFormValues,
  StudentRegistrationFormValues,
  ValidationErrors,
} from '@/types/models';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
/**
 * Matches Wachemo University Student ID formats such as:
 * WCU/1420/16, WCU/0982/15, WCU/UR/1234/16, or WCU-1420-16
 */
const WCU_STUDENT_ID_REGEX = /^WCU([\/\-][A-Z0-9]{2,6}){2,3}$/i;

export function validateLoginForm(
  values: LoginFormValues
): ValidationErrors<LoginFormValues> {
  const errors: ValidationErrors<LoginFormValues> = {};

  const trimmedEmail = values.email.trim();
  if (!trimmedEmail) {
    errors.email = 'Institutional or registered email address is required.';
  } else if (!EMAIL_REGEX.test(trimmedEmail)) {
    errors.email = 'Please enter a valid email address.';
  }

  if (!values.password) {
    errors.password = 'Password is required.';
  } else if (values.password.length < 6) {
    errors.password = 'Password must be at least 6 characters.';
  }

  return errors;
}

export function validateStudentRegistration(
  values: StudentRegistrationFormValues
): ValidationErrors<StudentRegistrationFormValues> {
  const errors: ValidationErrors<StudentRegistrationFormValues> = {};

  const trimmedName = values.fullName.trim();
  if (!trimmedName) {
    errors.fullName = 'Full legal name is required.';
  } else if (trimmedName.split(/\s+/).length < 2) {
    errors.fullName = 'Please enter both your first and father/last name.';
  }

  const trimmedStudentId = values.studentIdNumber.trim().toUpperCase();
  if (!trimmedStudentId) {
    errors.studentIdNumber = 'Wachemo University Student ID is required.';
  } else if (!WCU_STUDENT_ID_REGEX.test(trimmedStudentId)) {
    errors.studentIdNumber =
      'Use official WCU format (e.g., WCU/1420/16 or WCU/UR/1420/16).';
  }

  const trimmedEmail = values.email.trim();
  if (!trimmedEmail) {
    errors.email = 'Email address is required.';
  } else if (!EMAIL_REGEX.test(trimmedEmail)) {
    errors.email = 'Please enter a valid email address.';
  }

  if (!values.facultyId) {
    errors.facultyId = 'Please select your College / Faculty.';
  }

  if (!values.departmentId) {
    errors.departmentId = 'Please select your Academic Department.';
  }

  if (!values.yearOfStudy) {
    errors.yearOfStudy = 'Please select your current Year of Study.';
  }

  if (!values.password) {
    errors.password = 'Password is required.';
  } else if (values.password.length < 8) {
    errors.password = 'Password must be at least 8 characters long.';
  } else if (!/[A-Za-z]/.test(values.password) || !/[0-9]/.test(values.password)) {
    errors.password = 'Password must contain both letters and numbers.';
  }

  if (!values.confirmPassword) {
    errors.confirmPassword = 'Please confirm your password.';
  } else if (values.confirmPassword !== values.password) {
    errors.confirmPassword = 'Passwords do not match.';
  }

  return errors;
}

export function validatePasswordResetEmail(email: string): string | null {
  const trimmed = email.trim();
  if (!trimmed) {
    return 'Please enter the email address associated with your WCU account.';
  }
  if (!EMAIL_REGEX.test(trimmed)) {
    return 'Please enter a valid email address.';
  }
  return null;
}
