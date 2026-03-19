export function getScryfallImageUrl(
  setCode: string,
  collectorNumber: string,
  version: 'small' | 'normal' | 'large' = 'normal'
): string {
  return `https://api.scryfall.com/cards/${setCode.toLowerCase()}/${encodeURIComponent(collectorNumber)}?format=image&version=${version}`
}
