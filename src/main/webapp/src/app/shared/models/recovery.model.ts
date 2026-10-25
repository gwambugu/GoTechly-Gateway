/**
 * Deferred reconciler types, mirroring `RecoveryModel` and `DeferredRecovery` in Core.
 *
 * The reconciler exists because a core-banking posting can time out after the scheme has already
 * been given a definite answer. It drives core banking into line with that answer — so every row
 * here is a transaction where the two might not yet agree.
 */

/** What the scheme was told. Decides which repair runs, so it is never guessed. */
export type DeclaredOutcome =
  /** Reported as settled — core banking must end up holding the posting. */
  | 'SUCCESS'
  /** Reported as rejected — core banking must end up NOT holding it. */
  | 'FAILED'
  /** Reported as not-yet-known; the scheme will enquire. Resolved, never repaired. */
  | 'PENDING'
  /** Could not be established. Escalated rather than guessed — an operator must supply it. */
  | 'UNKNOWN';

/** What core banking holds, as established by the gate (`cbc-txn-status`). */
export type CbsState = 'POSTED' | 'NOT_POSTED' | 'UNKNOWN';

/** Lifecycle. Only the first two are live. */
export type RecoveryState =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'RECONCILED'
  /** Core banking does not match what the scheme was told, and only a human can close it. */
  | 'NEEDS_ATTENTION'
  | 'CANCELLED';

export type RecoveryAction = 'NOTHING_TO_DO' | 'REVERSE' | 'REPOST' | 'RETRY';

export interface DeferredRecovery {
  id: number;
  requestId: string;
  paymentType: string;
  direction: string;
  declaredOutcome: DeclaredOutcome;
  state: RecoveryState;
  lastCbsState?: CbsState;
  lastAction?: RecoveryAction;
  triggerHaltCode?: string;
  lastPhase?: string;
  lastDetail?: string;
  attempts: number;
  nextAttemptAt: string;
  deadlineAt: string;
  ownerInstance?: string;
  claimedAt?: string;
  createdAt: string;
  updatedAt: string;
}

/** In-memory working-set numbers for the instance that answered the request. */
export interface QueueMetrics {
  running: boolean;
  queued: number;
  capacity: number;
  highWaterMark: number;
  /**
   * Offers the queue could not take because it was full. Never lost — the durable row stays and the
   * sweeper picks it up — so this is a pressure signal, not an error count.
   */
  deferredOffers: number;
  attemptsRun: number;
  workers: number;
  instanceId: string;
}

export interface RecoveryMetrics {
  states: Record<RecoveryState, number>;
  /** The one number worth alerting on. */
  needsAttention: number;
  queue: QueueMetrics;
  asOf: string;
}
