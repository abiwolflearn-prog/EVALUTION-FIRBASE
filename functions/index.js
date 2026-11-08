/**
 * Wachemo University Teacher Evaluation System
 * Privileged Server-Side Firebase Cloud Functions (Admin SDK)
 *
 * Security Invariants:
 * 1. Never bundle service account keys in the Android client APK.
 * 2. Provisioning a Department Admin executes server-side via Firebase Admin SDK
 *    (or Super Admin Firestore authorization registry) so the Super Admin's active
 *    client FirebaseAuth session is NEVER replaced.
 * 3. Every administrative mutation verifies that the caller is an active Super Admin
 *    and writes an immutable record to /audit_logs/{logId} with server timestamp.
 */

const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');

if (!admin.apps.length) {
  admin.initializeApp();
}

const BOOTSTRAPPED_SUPER_ADMIN_EMAIL = 'abelbimrew868@gmail.com';

async function verifyActiveSuperAdmin(db, authContext) {
  if (!authContext || !authContext.uid) {
    throw new HttpsError('unauthenticated', 'Authentication is required.');
  }

  const callerUid = authContext.uid;
  const adminSnap = await db.collection('admins').doc(callerUid).get();
  const adminData = adminSnap.exists ? adminSnap.data() : null;

  if (adminData && adminData.isActive === false) {
    throw new HttpsError(
      'permission-denied',
      'Administrator account is deactivated.'
    );
  }

  const isBootstrapped =
    authContext.token &&
    authContext.token.email === BOOTSTRAPPED_SUPER_ADMIN_EMAIL &&
    authContext.token.email_verified === true;

  const isSuperAdminDoc =
    adminData &&
    adminData.isActive !== false &&
    (!adminData.role ||
      adminData.role === 'super_admin' ||
      adminData.role === 'admin');

  if (!isBootstrapped && !isSuperAdminDoc) {
    throw new HttpsError(
      'permission-denied',
      'Only an active Super Admin can invoke this operation.'
    );
  }

  return callerUid;
}

exports.provisionDepartmentAdmin = onCall(async (request) => {
  const db = admin.firestore();
  const callerUid = await verifyActiveSuperAdmin(db, request.auth);

  const {
    targetUid,
    fullName,
    email,
    facultyId,
    departmentId,
  } = request.data || {};

  if (!fullName || typeof fullName !== 'string' || fullName.trim().length < 2) {
    throw new HttpsError('invalid-argument', 'Valid fullName is required.');
  }
  if (!email || typeof email !== 'string' || !email.includes('@')) {
    throw new HttpsError('invalid-argument', 'Valid email is required.');
  }
  if (!facultyId || !departmentId) {
    throw new HttpsError(
      'invalid-argument',
      'Both facultyId and departmentId are required for a Department Admin.'
    );
  }

  // Validate faculty and department relationship on the server
  const [facSnap, deptSnap] = await Promise.all([
    db.collection('faculties').doc(facultyId).get(),
    db.collection('departments').doc(departmentId).get(),
  ]);

  if (!facSnap.exists || !deptSnap.exists) {
    throw new HttpsError(
      'failed-precondition',
      'Selected faculty or department does not exist.'
    );
  }
  if (deptSnap.data().facultyId !== facultyId) {
    throw new HttpsError(
      'failed-precondition',
      'Selected department does not belong to the selected faculty.'
    );
  }

  // Resolve or create Firebase Auth user via Admin SDK without touching caller session
  let resolvedUid = targetUid;
  if (!resolvedUid) {
    try {
      const existingUser = await admin.auth().getUserByEmail(email.trim());
      resolvedUid = existingUser.uid;
    } catch (_) {
      const createdUser = await admin.auth().createUser({
        email: email.trim(),
        displayName: fullName.trim(),
        emailVerified: true,
      });
      resolvedUid = createdUser.uid;
    }
  }

  if (resolvedUid === callerUid) {
    throw new HttpsError(
      'failed-precondition',
      'Super Admin cannot overwrite their own authorization record.'
    );
  }

  const now = admin.firestore.FieldValue.serverTimestamp();
  const adminRef = db.collection('admins').doc(resolvedUid);
  const auditRef = db.collection('audit_logs').doc();

  await db.runTransaction(async (tx) => {
    tx.set(adminRef, {
      uid: resolvedUid,
      fullName: fullName.trim(),
      email: email.trim(),
      role: 'department_admin',
      facultyId: facultyId.trim(),
      departmentId: departmentId.trim(),
      isActive: true,
      createdBy: callerUid,
      createdAt: now,
      updatedAt: now,
    });

    tx.set(auditRef, {
      id: auditRef.id,
      actorUid: callerUid,
      actorRole: 'super_admin',
      assignedDepartmentId: '',
      action: 'CREATE_DEPARTMENT_ADMIN',
      targetCollection: 'admins',
      targetDocumentId: resolvedUid,
      details: `Provisioned Department Admin ${email.trim()} for ${departmentId}`,
      timestamp: now,
    });
  });

  return { uid: resolvedUid, role: 'department_admin', status: 'active' };
});
