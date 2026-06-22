/**
 * Types du domaine Academy (LMS-light + gamification, §19.3), miroir des DTO
 * exposés par api-quality-engine (/api/v1/academy/*).
 */

export type CourseStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export type AcademyEnrollmentStatus =
  | 'ENROLLED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export type LessonContentType = 'TEXT' | 'VIDEO' | 'SIMULATION' | 'EXTERNAL';

export interface CourseResponse {
  id: string;
  tenantId: string;
  code: string;
  title: string;
  description?: string | null;
  targetRole?: string | null;
  industrySector?: string | null;
  passingScore: number;
  pointsReward: number;
  validityMonths?: number | null;
  status: CourseStatus;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface LessonResponse {
  id: string;
  moduleId: string;
  title: string;
  contentType: LessonContentType;
  body?: string | null;
  mediaUrl?: string | null;
  durationMinutes: number;
  orderIndex: number;
  createdAt: string;
  updatedAt: string;
}

export interface QuestionForLearner {
  id: string;
  stem: string;
  options: string[];
  points: number;
  orderIndex: number;
}

export interface QuizForLearner {
  id: string;
  moduleId: string;
  title: string;
  passScore: number;
  questions: QuestionForLearner[];
}

export interface ModuleResponse {
  id: string;
  courseId: string;
  title: string;
  summary?: string | null;
  orderIndex: number;
  createdAt: string;
  updatedAt: string;
}

export interface ModuleOutline {
  module: ModuleResponse;
  lessons: LessonResponse[];
  quiz?: QuizForLearner | null;
}

export interface CourseOutline {
  course: CourseResponse;
  modules: ModuleOutline[];
}

export interface EnrollmentResponse {
  id: string;
  tenantId: string;
  userId: string;
  courseId: string;
  status: AcademyEnrollmentStatus;
  progressPct: number;
  finalScore?: number | null;
  enrolledAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  expiresOn?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface QuizResult {
  attemptId: string;
  quizId: string;
  score: number;
  passed: boolean;
  earnedPoints: number;
  totalPoints: number;
  enrollment: EnrollmentResponse;
}

export interface CertificateResponse {
  id: string;
  enrollmentId: string;
  courseId: string;
  code: string;
  courseCode: string;
  courseTitle: string;
  finalScore: number;
  sha256: string;
  anchorTxRef?: string | null;
  issuedAt: string;
  expiresOn?: string | null;
  verifyUrl: string;
  htmlContent: string;
}

export interface CertificateVerification {
  code: string;
  valid: boolean;
  courseCode: string;
  courseTitle: string;
  finalScore: number;
  issuedAt: string;
  expiresOn?: string | null;
  sha256: string;
  anchorTxRef?: string | null;
  signatureValid: boolean;
}

export interface LeaderboardEntry {
  rank: number;
  userId: string;
  points: number;
  completedCount: number;
  bestScore?: number | null;
  beltLevel: string;
  badges: string[];
}

export interface Leaderboard {
  entries: LeaderboardEntry[];
  totalLearners: number;
}

/** Page Spring (sous-ensemble utilisé). */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
