/**
 * Wachemo University Teacher Evaluation System
 * Core Domain & TypeScript Interfaces
 */

export type UserRole = 'student' | 'admin';

export type RatingValue = 1 | 2 | 3 | 4 | 5;

export type EvaluationCategoryKey =
  | 'teachingQuality'
  | 'communicationSkills'
  | 'punctualityAndPreparation'
  | 'courseManagement'
  | 'studentSupport'
  | 'professionalismAndEthics';

export interface UserProfile {
  uid: string;
  email: string;
  fullName: string;
  role: UserRole;
  avatarUrl?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StudentRecord {
  uid: string;
  studentIdNumber: string; // e.g. WCU/1420/16
  fullName: string;
  email: string;
  facultyId: string;
  departmentId: string;
  yearOfStudy: number;
  semesterId: string;
  createdAt: string;
}

export interface Faculty {
  id: string;
  code: string;
  name: string;
  deanName?: string;
  description: string;
}

export interface Department {
  id: string;
  facultyId: string;
  code: string;
  name: string;
  headOfDepartment?: string;
}

export interface TeacherRecord {
  id: string;
  employeeId: string;
  fullName: string;
  academicRank:
    | 'Graduate Assistant'
    | 'Assistant Lecturer'
    | 'Lecturer'
    | 'Assistant Professor'
    | 'Associate Professor'
    | 'Professor';
  email: string;
  facultyId: string;
  departmentId: string;
  specialization: string;
  courseIds: string[];
  isActive: boolean;
}

export interface Course {
  id: string;
  code: string;
  title: string;
  creditHours: number;
  facultyId: string;
  departmentId: string;
  semesterNumber: 1 | 2;
  yearLevel: number;
}

export interface AcademicSemester {
  id: string;
  academicYear: string; // e.g., "2026/2027"
  semesterName: 'Semester I' | 'Semester II';
  isEvaluationOpen: boolean;
  startDate: string;
  endDate: string;
}

export interface EvaluationQuestionDefinition {
  id: string;
  categoryKey: EvaluationCategoryKey;
  prompt: string;
}

export interface EvaluationCategoryDefinition {
  key: EvaluationCategoryKey;
  number: number;
  title: string;
  description: string;
  questions: EvaluationQuestionDefinition[];
}

export interface EvaluationRecord {
  id: string; // Deterministic ID: `${studentUid}_${teacherId}_${courseId}_${semesterId}`
  studentId: string;
  teacherId: string;
  courseId: string;
  semesterId: string;
  academicYear: string;
  categories: Record<EvaluationCategoryKey, number>;
  questionRatings: Record<string, RatingValue>;
  overallScore: number;
  comment?: string;
  submittedAt: string;
  status: 'submitted' | 'locked';
}

export interface NotificationItem {
  id: string;
  recipientUid: string;
  title: string;
  body: string;
  type: 'evaluation_period' | 'submission_confirmation' | 'system';
  isRead: boolean;
  createdAt: string;
}

export interface LoginFormValues {
  email: string;
  password: string;
  intendedRole: UserRole;
}

export interface StudentRegistrationFormValues {
  fullName: string;
  studentIdNumber: string;
  email: string;
  facultyId: string;
  departmentId: string;
  yearOfStudy: string;
  password: string;
  confirmPassword: string;
}

export type ValidationErrors<T> = Partial<Record<keyof T, string>>;
