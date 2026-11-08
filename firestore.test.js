const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const { test, before, after, beforeEach } = require('node:test');
const fs = require('node:fs');

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || 'demo-no-project';
const STUDENT_ALICE_UID = 'student_alice_123';
const STUDENT_BOB_UID = 'student_bob_456';
const ADMIN_UID = 'admin_wcu_001';

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085'
).split(':');
const emulatorPort = parseInt(emulatorPortStr, 10);

function validStudentPayload(uid, overrides = {}) {
  return {
    uid,
    fullName: 'Abebe Kebede',
    email: 'abebe.kebede@wcu.edu.et',
    studentId: 'WCU/1420/16',
    role: 'student',
    facultyId: 'fac_eng_tech',
    departmentId: 'dep_se',
    yearOfStudy: 3,
    isActive: true,
    createdAt: new Date(Date.now() - 1000),
    updatedAt: new Date(Date.now() - 1000),
    ...overrides,
  };
}

function validEvaluationPayload(evaluationId, studentId, overrides = {}) {
  const categoryRatings = {
    cat_1: 4.67,
    cat_2: 4.33,
    cat_3: 5.0,
    cat_4: 4.67,
    cat_5: 4.33,
    cat_6: 5.0,
  };
  const questionRatings = {};
  for (let i = 1; i <= 18; i++) {
    questionRatings[`q_${i}`] = 5;
  }
  return {
    evaluationId,
    studentId,
    teacherId: 'tch_01',
    courseId: 'crs_se301',
    departmentId: 'dep_se',
    facultyId: 'fac_eng_tech',
    semesterId: 'sem_2026_1',
    academicYear: '2026/27',
    categoryRatings,
    questionRatings,
    overallScore: 4.67,
    comment: 'Clear explanations and well-organized lab sessions.',
    submittedAt: new Date(Date.now() - 500),
    status: 'submitted',
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync('./firestore.rules', 'utf8');
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test('1. Unauthenticated user cannot read or create user profiles', async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection('users').doc(STUDENT_ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection('users')
      .doc(STUDENT_ALICE_UID)
      .set(validStudentPayload(STUDENT_ALICE_UID))
  );
});

test('2. Authenticated student can create their own valid student profile', async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection('users')
      .doc(STUDENT_ALICE_UID)
      .set(validStudentPayload(STUDENT_ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection('users').doc(STUDENT_ALICE_UID).get()
  );
});

test('3. Student role escalation prevention: student cannot self-assign admin role on create', async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection('users')
      .doc(STUDENT_ALICE_UID)
      .set(validStudentPayload(STUDENT_ALICE_UID, { role: 'admin' }))
  );
});

test('4. Student role escalation prevention: student cannot escalate role to admin on update', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection('users')
      .doc(STUDENT_ALICE_UID)
      .set(validStudentPayload(STUDENT_ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection('users').doc(STUDENT_ALICE_UID).update({
      role: 'admin',
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test('5. Student can update own permitted profile fields (fullName, departmentId, yearOfStudy)', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection('users')
      .doc(STUDENT_ALICE_UID)
      .set(validStudentPayload(STUDENT_ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection('users').doc(STUDENT_ALICE_UID).update({
      fullName: 'Abebe Kebede Tadesse',
      yearOfStudy: 4,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test('6. PII isolation: Student cannot read or update another student profile', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection('users')
      .doc(STUDENT_BOB_UID)
      .set(validStudentPayload(STUDENT_BOB_UID, { email: 'bob@wcu.edu.et' }));
  });

  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();
  await assertFails(aliceDb.collection('users').doc(STUDENT_BOB_UID).get());
  await assertFails(
    aliceDb.collection('users').doc(STUDENT_BOB_UID).update({
      fullName: 'Tampered Name',
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test('7. Query Enforcer: Student cannot list all users without uid filter', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection('users')
      .doc(STUDENT_ALICE_UID)
      .set(validStudentPayload(STUDENT_ALICE_UID));
    await ctx
      .firestore()
      .collection('users')
      .doc(STUDENT_BOB_UID)
      .set(validStudentPayload(STUDENT_BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();
  await assertFails(aliceDb.collection('users').get());
  await assertSucceeds(
    aliceDb.collection('users').where('uid', '==', STUDENT_ALICE_UID).get()
  );
});

test('8. Shadow update rejection: Student cannot inject unauthorized extra fields', async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection('users')
      .doc(STUDENT_ALICE_UID)
      .set(validStudentPayload(STUDENT_ALICE_UID, { isSuperUser: true }))
  );
});

test('9. Unauthorized admin registry access: Student cannot write to /admins/{uid}', async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection('admins').doc(STUDENT_ALICE_UID).set({
      uid: STUDENT_ALICE_UID,
      email: 'abebe.kebede@wcu.edu.et',
      createdAt: new Date(Date.now() - 500),
    })
  );
});

test('10. Authorized Admin (in /admins/{uid}) can read and manage student profiles', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('admins').doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: 'admin.qa@wcu.edu.et',
      createdAt: new Date(Date.now() - 1000),
    });
    await ctx
      .firestore()
      .collection('users')
      .doc(STUDENT_ALICE_UID)
      .set(validStudentPayload(STUDENT_ALICE_UID));
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(adminDb.collection('users').doc(STUDENT_ALICE_UID).get());
  await assertSucceeds(adminDb.collection('users').get());
  await assertSucceeds(
    adminDb.collection('users').doc(STUDENT_ALICE_UID).update({
      isActive: false,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test('11. Phase 3A: Academic collections allow authenticated read but restrict writes to Admin', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('admins').doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: 'admin.qa@wcu.edu.et',
      createdAt: new Date(Date.now() - 1000),
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();

  const teacherDoc = {
    id: 'tch_01',
    fullName: 'Dr. Tesfaye Worku',
    email: 'tesfaye.worku@wcu.edu.et',
    profileImage: '',
    facultyId: 'fac_eng_tech',
    departmentId: 'dep_se',
    specialization: 'Distributed Systems & Software Architecture',
    isActive: true,
  };

  await assertSucceeds(adminDb.collection('teachers').doc('tch_01').set(teacherDoc));
  await assertFails(aliceDb.collection('teachers').doc('tch_02').set({ ...teacherDoc, id: 'tch_02' }));
  await assertSucceeds(aliceDb.collection('teachers').doc('tch_01').get());
});

test('12. Phase 3A: Evaluation submission enforces department eligibility, composite ID duplicate prevention, and immutability', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection('users').doc(STUDENT_ALICE_UID).set(validStudentPayload(STUDENT_ALICE_UID));
    await db.collection('users').doc(STUDENT_BOB_UID).set(
      validStudentPayload(STUDENT_BOB_UID, { departmentId: 'dep_nurs' })
    );
    await db.collection('courses').doc('crs_se301').set({
      id: 'crs_se301',
      courseCode: 'SEng3011',
      courseName: 'Software Architecture',
      teacherId: 'tch_01',
      departmentId: 'dep_se',
      semesterId: 'sem_2026_1',
      academicYear: '2026/27',
      isActive: true,
    });
  });

  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(STUDENT_BOB_UID).firestore();

  const evalId = `${STUDENT_ALICE_UID}_tch_01_crs_se301_sem_2026_1`;

  // Alice (in dep_se) can submit her evaluation once
  await assertSucceeds(
    aliceDb
      .collection('evaluations')
      .doc(evalId)
      .set(validEvaluationPayload(evalId, STUDENT_ALICE_UID))
  );

  // Duplicate submission or rating modification is rejected (allow update: if false)
  await assertFails(
    aliceDb
      .collection('evaluations')
      .doc(evalId)
      .set(validEvaluationPayload(evalId, STUDENT_ALICE_UID, { overallScore: 1.0 }))
  );

  // Deletion of submitted evaluation is rejected
  await assertFails(aliceDb.collection('evaluations').doc(evalId).delete());

  // Bob (in dep_nurs) cannot read Alice's evaluation and cannot evaluate a dep_se course
  await assertFails(bobDb.collection('evaluations').doc(evalId).get());
  const bobEvalId = `${STUDENT_BOB_UID}_tch_01_crs_se301_sem_2026_1`;
  await assertFails(
    bobDb
      .collection('evaluations')
      .doc(bobEvalId)
      .set(validEvaluationPayload(bobEvalId, STUDENT_BOB_UID))
  );
});

test('13. Super Admin can create and assign a Department Admin after verifying faculty and department exist', async () => {
  const DEPT_ADMIN_SE_UID = 'dept_admin_se_001';
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection('admins').doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      fullName: 'University Super Admin',
      email: 'super.admin@wcu.edu.et',
      role: 'super_admin',
      isActive: true,
      createdAt: new Date(Date.now() - 2000),
    });
    await db.collection('faculties').doc('fac_eng_tech').set({
      id: 'fac_eng_tech',
      name: 'College of Engineering and Technology',
      description: 'Engineering programs',
      isActive: true,
    });
    await db.collection('departments').doc('dep_se').set({
      id: 'dep_se',
      name: 'Software Engineering',
      facultyId: 'fac_eng_tech',
      description: 'SE department',
      isActive: true,
    });
  });

  const superAdminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(
    superAdminDb.collection('admins').doc(DEPT_ADMIN_SE_UID).set({
      uid: DEPT_ADMIN_SE_UID,
      fullName: 'Dr. Dawit Bekele',
      email: 'dawit.bekele@wcu.edu.et',
      role: 'department_admin',
      facultyId: 'fac_eng_tech',
      departmentId: 'dep_se',
      isActive: true,
      createdBy: ADMIN_UID,
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Rejects assigning a department that does NOT belong to the specified faculty
  await assertFails(
    superAdminDb.collection('admins').doc('dept_admin_invalid').set({
      uid: 'dept_admin_invalid',
      fullName: 'Invalid Dept Admin',
      email: 'invalid@wcu.edu.et',
      role: 'department_admin',
      facultyId: 'fac_med_health',
      departmentId: 'dep_se',
      isActive: true,
      createdBy: ADMIN_UID,
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test('14. Department Admin can read/manage ONLY their assigned department and is blocked from other departments', async () => {
  const DEPT_ADMIN_SE_UID = 'dept_admin_se_001';
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection('admins').doc(DEPT_ADMIN_SE_UID).set({
      uid: DEPT_ADMIN_SE_UID,
      fullName: 'Dr. Dawit Bekele',
      email: 'dawit.bekele@wcu.edu.et',
      role: 'department_admin',
      facultyId: 'fac_eng_tech',
      departmentId: 'dep_se',
      isActive: true,
      createdBy: ADMIN_UID,
      createdAt: new Date(Date.now() - 1000),
    });
    await db.collection('users').doc(STUDENT_ALICE_UID).set(
      validStudentPayload(STUDENT_ALICE_UID, { departmentId: 'dep_se', facultyId: 'fac_eng_tech' })
    );
    await db.collection('users').doc(STUDENT_BOB_UID).set(
      validStudentPayload(STUDENT_BOB_UID, { departmentId: 'dep_cs', facultyId: 'fac_eng_tech' })
    );
    await db.collection('evaluations').doc('eval_se_1').set(
      validEvaluationPayload('eval_se_1', STUDENT_ALICE_UID, { departmentId: 'dep_se', facultyId: 'fac_eng_tech' })
    );
    await db.collection('evaluations').doc('eval_cs_1').set(
      validEvaluationPayload('eval_cs_1', STUDENT_BOB_UID, { departmentId: 'dep_cs', facultyId: 'fac_eng_tech' })
    );
  });

  const seAdminDb = testEnv.authenticatedContext(DEPT_ADMIN_SE_UID).firestore();

  // Assigned department (dep_se) student & evaluation reads succeed
  await assertSucceeds(seAdminDb.collection('users').doc(STUDENT_ALICE_UID).get());
  await assertSucceeds(seAdminDb.collection('evaluations').doc('eval_se_1').get());

  // Cross-department (dep_cs) student & evaluation reads fail
  await assertFails(seAdminDb.collection('users').doc(STUDENT_BOB_UID).get());
  await assertFails(seAdminDb.collection('evaluations').doc('eval_cs_1').get());

  // Assigned department teacher & course writes succeed
  await assertSucceeds(
    seAdminDb.collection('teachers').doc('tch_se_99').set({
      id: 'tch_se_99',
      fullName: 'Dr. Solomon Tadesse',
      email: 'solomon@wcu.edu.et',
      profileImage: '',
      facultyId: 'fac_eng_tech',
      departmentId: 'dep_se',
      specialization: 'Software Testing',
      isActive: true,
    })
  );
  await assertSucceeds(
    seAdminDb.collection('courses').doc('crs_se_99').set({
      id: 'crs_se_99',
      courseCode: 'SENG499',
      courseName: 'Software Quality Engineering',
      teacherId: 'tch_se_99',
      departmentId: 'dep_se',
      semesterId: 'sem_2026_1',
      academicYear: '2026/27',
      isActive: true,
    })
  );

  // Cross-department teacher & course writes fail
  await assertFails(
    seAdminDb.collection('teachers').doc('tch_cs_99').set({
      id: 'tch_cs_99',
      fullName: 'Dr. Other Dept',
      email: 'other@wcu.edu.et',
      profileImage: '',
      facultyId: 'fac_eng_tech',
      departmentId: 'dep_cs',
      specialization: 'AI',
      isActive: true,
    })
  );
  await assertFails(
    seAdminDb.collection('courses').doc('crs_cs_99').set({
      id: 'crs_cs_99',
      courseCode: 'COSC499',
      courseName: 'Compilers',
      teacherId: 'tch_cs_99',
      departmentId: 'dep_cs',
      semesterId: 'sem_2026_1',
      academicYear: '2026/27',
      isActive: true,
    })
  );
});

test('15. Department Admin cannot create/modify administrators or change their own assigned department', async () => {
  const DEPT_ADMIN_SE_UID = 'dept_admin_se_001';
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('admins').doc(DEPT_ADMIN_SE_UID).set({
      uid: DEPT_ADMIN_SE_UID,
      fullName: 'Dr. Dawit Bekele',
      email: 'dawit.bekele@wcu.edu.et',
      role: 'department_admin',
      facultyId: 'fac_eng_tech',
      departmentId: 'dep_se',
      isActive: true,
      createdBy: ADMIN_UID,
      createdAt: new Date(Date.now() - 1000),
    });
  });

  const seAdminDb = testEnv.authenticatedContext(DEPT_ADMIN_SE_UID).firestore();

  // Can read own /admins/{uid} record
  await assertSucceeds(seAdminDb.collection('admins').doc(DEPT_ADMIN_SE_UID).get());

  // Cannot create another admin
  await assertFails(
    seAdminDb.collection('admins').doc('new_admin_99').set({
      uid: 'new_admin_99',
      fullName: 'Unauthorized Admin',
      email: 'unauth@wcu.edu.et',
      role: 'department_admin',
      facultyId: 'fac_eng_tech',
      departmentId: 'dep_se',
      isActive: true,
      createdBy: DEPT_ADMIN_SE_UID,
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cannot modify own assigned department or escalate to super_admin
  await assertFails(
    seAdminDb.collection('admins').doc(DEPT_ADMIN_SE_UID).update({
      departmentId: 'dep_cs',
    })
  );
  await assertFails(
    seAdminDb.collection('admins').doc(DEPT_ADMIN_SE_UID).update({
      role: 'super_admin',
    })
  );
});

test('16. Inactive administrator (isActive == false) cannot perform any admin actions', async () => {
  const INACTIVE_ADMIN_UID = 'inactive_admin_001';
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection('admins').doc(INACTIVE_ADMIN_UID).set({
      uid: INACTIVE_ADMIN_UID,
      fullName: 'Former Dept Admin',
      email: 'former@wcu.edu.et',
      role: 'department_admin',
      facultyId: 'fac_eng_tech',
      departmentId: 'dep_se',
      isActive: false,
      createdBy: ADMIN_UID,
      createdAt: new Date(Date.now() - 1000),
    });
    await db.collection('users').doc(STUDENT_ALICE_UID).set(
      validStudentPayload(STUDENT_ALICE_UID, { departmentId: 'dep_se' })
    );
  });

  const inactiveAdminDb = testEnv.authenticatedContext(INACTIVE_ADMIN_UID).firestore();
  await assertFails(inactiveAdminDb.collection('users').doc(STUDENT_ALICE_UID).get());
  await assertFails(
    inactiveAdminDb.collection('teachers').doc('tch_se_blocked').set({
      id: 'tch_se_blocked',
      fullName: 'Blocked Write',
      email: 'blocked@wcu.edu.et',
      profileImage: '',
      facultyId: 'fac_eng_tech',
      departmentId: 'dep_se',
      specialization: 'None',
      isActive: true,
    })
  );
});

test('17. Phase 5: Export audit logs enforce role, department scope, and valid exportFormat', async () => {
  const DEPT_ADMIN_SE_UID = 'dept_admin_se_001';
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection('admins').doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      fullName: 'University Super Admin',
      email: 'super.admin@wcu.edu.et',
      role: 'super_admin',
      isActive: true,
      createdAt: new Date(Date.now() - 2000),
    });
    await db.collection('admins').doc(DEPT_ADMIN_SE_UID).set({
      uid: DEPT_ADMIN_SE_UID,
      fullName: 'Dr. Dawit Bekele',
      email: 'dawit.bekele@wcu.edu.et',
      role: 'department_admin',
      facultyId: 'fac_eng_tech',
      departmentId: 'dep_se',
      isActive: true,
      createdBy: ADMIN_UID,
      createdAt: new Date(Date.now() - 1000),
    });
  });

  const superAdminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const seAdminDb = testEnv.authenticatedContext(DEPT_ADMIN_SE_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE_UID).firestore();

  // Super Admin can record university-wide PDF export audit log
  await assertSucceeds(
    superAdminDb.collection('audit_logs').doc('exp_log_super_1').set({
      id: 'exp_log_super_1',
      actorUid: ADMIN_UID,
      actorRole: 'super_admin',
      assignedDepartmentId: '',
      action: 'EXPORT_EVALUATION_REPORT_PDF',
      targetCollection: 'evaluations',
      targetDocumentId: 'UNIVERSITY_WIDE',
      exportFormat: 'PDF',
      reportScope: 'UNIVERSITY_WIDE',
      selectedFilters: 'ALL (Unfiltered)',
      exportedRecordCount: 42,
      details: 'Exported university-wide PDF report',
      timestamp: new Date(Date.now() - 200),
    })
  );

  // Department Admin can record own-department XLSX export audit log
  await assertSucceeds(
    seAdminDb.collection('audit_logs').doc('exp_log_dept_1').set({
      id: 'exp_log_dept_1',
      actorUid: DEPT_ADMIN_SE_UID,
      actorRole: 'department_admin',
      assignedDepartmentId: 'dep_se',
      action: 'EXPORT_EVALUATION_REPORT_XLSX',
      targetCollection: 'evaluations',
      targetDocumentId: 'dep_se',
      exportFormat: 'XLSX',
      reportScope: 'DEPARTMENT:dep_se',
      selectedFilters: 'faculty=fac_eng_tech, department=dep_se',
      exportedRecordCount: 14,
      details: 'Exported department XLSX report',
      timestamp: new Date(Date.now() - 200),
    })
  );

  // Department Admin CANNOT forge an audit log for another department
  await assertFails(
    seAdminDb.collection('audit_logs').doc('exp_log_dept_forged').set({
      id: 'exp_log_dept_forged',
      actorUid: DEPT_ADMIN_SE_UID,
      actorRole: 'department_admin',
      assignedDepartmentId: 'dep_cs',
      action: 'EXPORT_EVALUATION_REPORT_PDF',
      targetCollection: 'evaluations',
      targetDocumentId: 'dep_cs',
      exportFormat: 'PDF',
      reportScope: 'DEPARTMENT:dep_cs',
      selectedFilters: 'department=dep_cs',
      exportedRecordCount: 10,
      details: 'Forged cross-department export log',
      timestamp: new Date(Date.now() - 200),
    })
  );

  // Student CANNOT write or read export audit logs
  await assertFails(
    aliceDb.collection('audit_logs').doc('exp_log_student').set({
      id: 'exp_log_student',
      actorUid: STUDENT_ALICE_UID,
      actorRole: 'student',
      assignedDepartmentId: 'dep_se',
      action: 'EXPORT_EVALUATION_REPORT_PDF',
      targetCollection: 'evaluations',
      targetDocumentId: 'dep_se',
      exportFormat: 'PDF',
      reportScope: 'DEPARTMENT:dep_se',
      selectedFilters: 'ALL',
      exportedRecordCount: 1,
      details: 'Student export attempt',
      timestamp: new Date(Date.now() - 200),
    })
  );
  await assertFails(aliceDb.collection('audit_logs').doc('exp_log_dept_1').get());
});


