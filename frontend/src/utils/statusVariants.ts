/**
 * Get badge variant based on status string.
 */
export function getStatusBadgeVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
  switch (status) {
    case 'ACTIVE':
    case 'APPROVED':
    case 'COMPLETED':
      return 'default';
    case 'PENDING':
    case 'REVIEW':
    case 'INACTIVE':
    case 'MAINTENANCE':
      return 'secondary';
    case 'SUSPENDED':
    case 'REJECTED':
    case 'CANCELLED':
    case 'TERMINATED':
    case 'FAILED':
    case 'DELETED':
      return 'destructive';
    default:
      return 'outline';
  }
}
