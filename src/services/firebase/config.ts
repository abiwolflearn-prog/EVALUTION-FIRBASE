import Constants from 'expo-constants';

/**
 * Centralized Firebase Public Client Configuration
 * Reads from EXPO_PUBLIC_FIREBASE_* environment variables (configured in .env).
 * Full Firebase Auth (initializeAuth with getReactNativePersistence) and Firestore
 * initialization are wired in Phase 2.
 */
export interface FirebaseClientConfig {
  apiKey: string;
  authDomain: string;
  projectId: string;
  storageBucket: string;
  messagingSenderId: string;
  appId: string;
}

export function getFirebaseClientConfig(): FirebaseClientConfig {
  const extraFirebase = Constants.expoConfig?.extra?.firebase as
    | Partial<FirebaseClientConfig>
    | undefined;

  return {
    apiKey:
      process.env.EXPO_PUBLIC_FIREBASE_API_KEY ?? extraFirebase?.apiKey ?? '',
    authDomain:
      process.env.EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN ??
      extraFirebase?.authDomain ??
      '',
    projectId:
      process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID ??
      extraFirebase?.projectId ??
      '',
    storageBucket:
      process.env.EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET ??
      extraFirebase?.storageBucket ??
      '',
    messagingSenderId:
      process.env.EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID ??
      extraFirebase?.messagingSenderId ??
      '',
    appId:
      process.env.EXPO_PUBLIC_FIREBASE_APP_ID ?? extraFirebase?.appId ?? '',
  };
}

export function isFirebaseConfigComplete(): boolean {
  const cfg = getFirebaseClientConfig();
  return Boolean(
    cfg.apiKey &&
      !cfg.apiKey.startsWith('YOUR_FIREBASE_') &&
      cfg.projectId &&
      !cfg.projectId.startsWith('YOUR_FIREBASE_') &&
      cfg.appId &&
      !cfg.appId.startsWith('YOUR_FIREBASE_')
  );
}
