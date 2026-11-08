import {
  LoginFormValues,
  StudentRegistrationFormValues,
  UserProfile,
} from '@/types/models';
import { isFirebaseConfigComplete } from '@/services/firebase/config';

/**
 * Auth Service Contract (Phase 1 Architecture Boundary)
 * Phase 2 connects these methods directly to Firebase Authentication
 * (`signInWithEmailAndPassword`, `createUserWithEmailAndPassword`, `sendPasswordResetEmail`)
 * and Cloud Firestore (`users/{uid}` and `students/{uid}`).
 */
export interface AuthOperationResult {
  success: boolean;
  message: string;
  user?: UserProfile;
}

export async function checkPhase1AuthReadiness(): Promise<{
  firebaseConfigured: boolean;
  phaseStatus: string;
}> {
  return {
    firebaseConfigured: isFirebaseConfigComplete(),
    phaseStatus: 'Phase 1 UI & Architecture Initialized — Ready for Phase 2 Firebase Auth Connection',
  };
}

export function formatPendingPhase2Message(actionLabel: string): string {
  if (!isFirebaseConfigComplete()) {
    return `${actionLabel} form validated successfully. Connect Firebase Authentication & EXPO_PUBLIC_FIREBASE_* credentials in Phase 2 to execute live authentication.`;
  }
  return `${actionLabel} validated. Ready for Phase 2 Firebase Auth handler.`;
}

export type { LoginFormValues, StudentRegistrationFormValues };
