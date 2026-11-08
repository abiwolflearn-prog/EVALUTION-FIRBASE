import React, { createContext, useContext, useMemo, useState } from 'react';
import { UserProfile, UserRole } from '@/types/models';

export interface AuthContextValue {
  user: UserProfile | null;
  role: UserRole | null;
  isLoading: boolean;
  statusMessage: string | null;
  errorMessage: string | null;
  clearMessages: () => void;
  setStatusMessage: (msg: string | null) => void;
  setErrorMessage: (msg: string | null) => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/**
 * AuthContext Provider
 * Phase 1 establishes the typed Auth state tree; Phase 2 attaches `onAuthStateChanged`
 * with AsyncStorage persistence and Firestore role verification.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user] = useState<UserProfile | null>(null);
  const [isLoading] = useState<boolean>(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const clearMessages = () => {
    setStatusMessage(null);
    setErrorMessage(null);
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      role: user?.role ?? null,
      isLoading,
      statusMessage,
      errorMessage,
      clearMessages,
      setStatusMessage,
      setErrorMessage,
    }),
    [user, isLoading, statusMessage, errorMessage]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuthContext(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuthContext must be used within an AuthProvider');
  }
  return context;
}
