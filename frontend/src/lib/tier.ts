export type Tier = 'green' | 'amber' | 'rose'

export const TIER_COLOR: Record<Tier, string> = {
  green: '#10B981',
  amber: '#F59E0B',
  rose: '#F43F5E',
}

export const TIER_CLASS: Record<Tier, { text: string; bg: string; dot: string; border: string }> = {
  green: { text: 'text-emerald-600', bg: 'bg-emerald-50', dot: 'bg-emerald-500', border: 'border-emerald-300' },
  amber: { text: 'text-amber-600', bg: 'bg-amber-50', dot: 'bg-amber-500', border: 'border-amber-300' },
  rose: { text: 'text-rose-600', bg: 'bg-rose-50', dot: 'bg-rose-500', border: 'border-rose-300' },
}

export function tierOf(walkingTimeSeconds: number): Tier {
  if (walkingTimeSeconds <= 60) return 'green'
  if (walkingTimeSeconds <= 180) return 'amber'
  return 'rose'
}
