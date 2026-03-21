export interface GameStartConfig {
  deckName: string
  aiDeckName: string | null
  format: string
  aiDifficulty: string
  pack1?: string  // Jumpstart only: full pack template ID
  pack2?: string  // Jumpstart only: full pack template ID
}
