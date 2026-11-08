import {
  Department,
  EvaluationCategoryDefinition,
  Faculty,
  RatingValue,
} from '@/types/models';

export const UNIVERSITY_INFO = {
  name: 'Wachemo University',
  shortName: 'WCU',
  systemTitle: 'Teacher Evaluation & Academic Quality Assurance System',
  campusLocation: 'Hossana, Ethiopia',
  motto: 'Light for the Generation!',
} as const;

export const RATING_SCALE_LABELS: Record<RatingValue, string> = {
  1: 'Very Poor',
  2: 'Poor',
  3: 'Average',
  4: 'Good',
  5: 'Excellent',
};

/**
 * Six official Wachemo University teacher evaluation categories and 18 criteria questions
 */
export const EVALUATION_CATEGORIES: EvaluationCategoryDefinition[] = [
  {
    key: 'teachingQuality',
    number: 1,
    title: 'Teaching Quality',
    description: 'Clarity of instruction, subject mastery, and pedagogical effectiveness.',
    questions: [
      {
        id: 'tq_1',
        categoryKey: 'teachingQuality',
        prompt: 'Explains concepts clearly.',
      },
      {
        id: 'tq_2',
        categoryKey: 'teachingQuality',
        prompt: 'Demonstrates subject knowledge.',
      },
      {
        id: 'tq_3',
        categoryKey: 'teachingQuality',
        prompt: 'Uses effective teaching methods.',
      },
    ],
  },
  {
    key: 'communicationSkills',
    number: 2,
    title: 'Communication Skills',
    description: 'Classroom engagement, clarity of expression, and responsiveness.',
    questions: [
      {
        id: 'cs_1',
        categoryKey: 'communicationSkills',
        prompt: 'Communicates effectively.',
      },
      {
        id: 'cs_2',
        categoryKey: 'communicationSkills',
        prompt: 'Encourages student participation.',
      },
      {
        id: 'cs_3',
        categoryKey: 'communicationSkills',
        prompt: 'Responds to student questions.',
      },
    ],
  },
  {
    key: 'punctualityAndPreparation',
    number: 3,
    title: 'Punctuality and Preparation',
    description: 'Adherence to class schedules, lesson pacing, and readiness.',
    questions: [
      {
        id: 'pp_1',
        categoryKey: 'punctualityAndPreparation',
        prompt: 'Attends classes on time.',
      },
      {
        id: 'pp_2',
        categoryKey: 'punctualityAndPreparation',
        prompt: 'Completes lessons according to schedule.',
      },
      {
        id: 'pp_3',
        categoryKey: 'punctualityAndPreparation',
        prompt: 'Comes prepared for classes.',
      },
    ],
  },
  {
    key: 'courseManagement',
    number: 4,
    title: 'Course Management',
    description: 'Syllabus coverage, instructional resources, and learning organization.',
    questions: [
      {
        id: 'cm_1',
        categoryKey: 'courseManagement',
        prompt: 'Covers the course syllabus.',
      },
      {
        id: 'cm_2',
        categoryKey: 'courseManagement',
        prompt: 'Provides appropriate learning materials.',
      },
      {
        id: 'cm_3',
        categoryKey: 'courseManagement',
        prompt: 'Organizes classroom activities effectively.',
      },
    ],
  },
  {
    key: 'studentSupport',
    number: 5,
    title: 'Student Support',
    description: 'Academic advising, inclusive support, and constructive assessment feedback.',
    questions: [
      {
        id: 'ss_1',
        categoryKey: 'studentSupport',
        prompt: 'Provides academic guidance.',
      },
      {
        id: 'ss_2',
        categoryKey: 'studentSupport',
        prompt: 'Supports students with learning difficulties.',
      },
      {
        id: 'ss_3',
        categoryKey: 'studentSupport',
        prompt: 'Gives constructive feedback.',
      },
    ],
  },
  {
    key: 'professionalismAndEthics',
    number: 6,
    title: 'Professionalism and Ethics',
    description: 'Fairness, professional conduct, and commitment to academic integrity.',
    questions: [
      {
        id: 'pe_1',
        categoryKey: 'professionalismAndEthics',
        prompt: 'Treats students fairly.',
      },
      {
        id: 'pe_2',
        categoryKey: 'professionalismAndEthics',
        prompt: 'Demonstrates professional behavior.',
      },
      {
        id: 'pe_3',
        categoryKey: 'professionalismAndEthics',
        prompt: 'Maintains academic integrity.',
      },
    ],
  },
];

export const WCU_FACULTIES: Faculty[] = [
  {
    id: 'fac_eng_tech',
    code: 'CET',
    name: 'College of Engineering and Technology',
    description: 'Computing, Electrical, Civil, Mechanical, and Chemical Engineering programs.',
  },
  {
    id: 'fac_nat_comp',
    code: 'CNCS',
    name: 'College of Natural and Computational Sciences',
    description: 'Mathematics, Physics, Chemistry, Biology, Statistics, and Sport Science.',
  },
  {
    id: 'fac_med_health',
    code: 'CMHS',
    name: 'College of Medicine and Health Sciences',
    description: 'Medicine, Public Health, Nursing, Midwifery, and Pharmacy.',
  },
  {
    id: 'fac_bus_econ',
    code: 'CBE',
    name: 'College of Business and Economics',
    description: 'Accounting, Economics, Management, Marketing, and Public Administration.',
  },
  {
    id: 'fac_soc_hum',
    code: 'CSSH',
    name: 'College of Social Sciences and Humanities',
    description: 'Law, English, Sociology, Geography, History, and Civics.',
  },
  {
    id: 'fac_agri',
    code: 'CANR',
    name: 'College of Agricultural Sciences',
    description: 'Plant Science, Animal Science, Natural Resource Management, and Horticulture.',
  },
];

export const WCU_DEPARTMENTS: Department[] = [
  {
    id: 'dep_se',
    facultyId: 'fac_eng_tech',
    code: 'SEng',
    name: 'Software Engineering',
  },
  {
    id: 'dep_cs',
    facultyId: 'fac_eng_tech',
    code: 'CS',
    name: 'Computer Science',
  },
  {
    id: 'dep_it',
    facultyId: 'fac_eng_tech',
    code: 'IT',
    name: 'Information Technology',
  },
  {
    id: 'dep_ece',
    facultyId: 'fac_eng_tech',
    code: 'ECE',
    name: 'Electrical and Computer Engineering',
  },
  {
    id: 'dep_civil',
    facultyId: 'fac_eng_tech',
    code: 'CEng',
    name: 'Civil Engineering',
  },
  {
    id: 'dep_math',
    facultyId: 'fac_nat_comp',
    code: 'MATH',
    name: 'Mathematics',
  },
  {
    id: 'dep_stat',
    facultyId: 'fac_nat_comp',
    code: 'STAT',
    name: 'Statistics',
  },
  {
    id: 'dep_nurs',
    facultyId: 'fac_med_health',
    code: 'NURS',
    name: 'Comprehensive Nursing',
  },
  {
    id: 'dep_ph',
    facultyId: 'fac_med_health',
    code: 'PH',
    name: 'Public Health Officer',
  },
  {
    id: 'dep_acct',
    facultyId: 'fac_bus_econ',
    code: 'ACCT',
    name: 'Accounting and Finance',
  },
  {
    id: 'dep_mgmt',
    facultyId: 'fac_bus_econ',
    code: 'MGMT',
    name: 'Management',
  },
  {
    id: 'dep_law',
    facultyId: 'fac_soc_hum',
    code: 'LAW',
    name: 'School of Law',
  },
  {
    id: 'dep_plant',
    facultyId: 'fac_agri',
    code: 'PLSC',
    name: 'Plant Science',
  },
];

export const STUDY_YEARS = [
  { label: 'Year 1 (Freshman)', value: '1' },
  { label: 'Year 2 (Sophomore)', value: '2' },
  { label: 'Year 3 (Junior)', value: '3' },
  { label: 'Year 4 (Senior)', value: '4' },
  { label: 'Year 5 (Graduating Engineering/Law)', value: '5' },
] as const;
