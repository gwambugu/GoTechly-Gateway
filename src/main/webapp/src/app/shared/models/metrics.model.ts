export interface RouteMetrics {
  routeId: string;
  totalRequests: number;
  avgLatencyMs: number;
  maxLatencyMs: number;
  errorCount: number;
}

export interface MetricsSummary {
  totalRequests: number;
  totalErrors: number;
  avgLatencyMs: number;
  successRate: number;
}
