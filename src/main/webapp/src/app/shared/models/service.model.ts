export type DrainState = 'ROUTABLE' | 'DRAINING' | 'UNKNOWN';

export interface ServiceHealth {
  name: string;
  baseUrl: string;
  /** Liveness only. Deliberately unaffected by drain — see drainState. */
  status: 'UP' | 'DOWN';
  responseTimeMs: number;
  lastChecked: string;
  error?: string;

  /** Whether the instance is accepting new work from the load balancer. */
  drainState: DrainState;
  /** Requests still running on the instance, including parked async ones. */
  inFlightHttp: number;
  oldestRequestAgeMs: number;
  /** Background work (EOD archival, a recovery repair) still executing plugin code. */
  activeBackgroundJobs: string[];
  /** Draining, nothing in flight, no background work — safe to deploy and restart. */
  quiet: boolean;
  /** Seconds in the current drain state — how a forgotten drain becomes visible. */
  stateForSeconds: number;
  /** Portal user who set the current state, or "unknown". */
  requestedBy: string;
  /** What inFlightHttp is counting, oldest first (capped at 20). */
  inFlightRequests?: NodeRequest[];
  /** Requests received since the drain began. Should stop growing within ~45s of the drain. */
  arrivalsWhileDraining?: number;
  /** Latest of those, newest first — remoteAddr identifies the route that bypassed the LB. */
  recentArrivalsWhileDraining?: NodeRequest[];
}

export interface NodeRequest {
  method: string | null;
  uri: string | null;
  /** TCP peer: 127.0.0.1 ⇒ plugin callback to core.base-url; the Gateway's IP ⇒ LB still routing here. */
  remoteAddr: string | null;
  forwardedFor: string | null;
  startedAt: string;
  ageMs: number;
}
