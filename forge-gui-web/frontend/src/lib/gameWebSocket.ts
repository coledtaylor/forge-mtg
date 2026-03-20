import { useGameStore } from '../stores/gameStore'
import type {
  OutboundMessage,
  InboundMessage,
  GameStateDto,
  GameLogEntry,
  ZoneUpdateDto,
  CombatDto,
  SpellAbilityDto,
  ButtonPayload,
  PromptPayload,
} from './gameTypes'

export class GameWebSocket {
  private ws: WebSocket | null = null
  private gameId: string
  private reconnectAttempts = 0
  private maxReconnectAttempts = 3
  private reconnectTimeouts = [1000, 2000, 4000]
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private started = false

  constructor(gameId: string) {
    this.gameId = gameId
  }

  private gameConfig?: {
    deckName: string
    aiDeckName: string | null
    format: string
    aiDifficulty: string
  }

  connect(gameConfig?: {
    deckName: string
    aiDeckName: string | null
    format: string
    aiDifficulty: string
  }): void {
    if (gameConfig) {
      this.gameConfig = gameConfig
    }
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    this.ws = new WebSocket(`${protocol}//${location.host}/ws/game/${this.gameId}`)
    const store = useGameStore.getState()

    this.ws.onopen = () => {
      console.log('[WS] Connected to game', this.gameId)
      useGameStore.getState().setConnected(true)
      this.reconnectAttempts = 0
      if (this.gameConfig && !this.started) {
        this.started = true
        this.sendStartGame(this.gameConfig)
      }
    }

    this.ws.onclose = (event: CloseEvent) => {
      console.log('[WS] Connection closed:', event.code, event.reason, 'wasClean:', event.wasClean)
      useGameStore.getState().setConnected(false)
      this.reconnect()
    }

    this.ws.onerror = (event: Event) => {
      console.error('[WS] Error:', event)
      store.setError('WebSocket connection error')
    }

    this.ws.onmessage = (event: MessageEvent) => {
      const msg = JSON.parse(event.data as string) as OutboundMessage
      console.log('[WS] Received:', msg.type, msg.inputId ? `(inputId=${msg.inputId})` : '')
      const s = useGameStore.getState()

      switch (msg.type) {
        case 'GAME_STATE': {
          const gs = msg.payload as GameStateDto
          console.log('[WS] GAME_STATE: players=', gs.players?.length, 'cards=', gs.cards?.length,
            'zones=', gs.players?.map(p => ({ name: p.name, zones: Object.fromEntries(Object.entries(p.zones).map(([k,v]) => [k, (v as number[]).length])) })))
          s.clearButtons()
          // Note: do NOT clearGameLog here. GAME_STATE arrives on every zone/card
          // update and would wipe accumulated log entries. The log is additive;
          // it is only cleared on full reset (disconnect/new game).
          s.applyGameState(gs)
          break
        }
        case 'ZONE_UPDATE':
          s.applyZoneUpdate(msg.payload as ZoneUpdateDto[])
          break
        case 'PHASE_UPDATE':
          s.applyPhaseUpdate((msg.payload as { phase: string }).phase)
          break
        case 'TURN_UPDATE':
          s.applyTurnUpdate(msg.payload as { turn: number; activePlayerId: number })
          break
        case 'COMBAT_UPDATE':
          s.applyCombatUpdate(msg.payload as CombatDto | null)
          break
        case 'STACK_UPDATE':
          s.applyStackUpdate(msg.payload as SpellAbilityDto[])
          break
        case 'BUTTON_UPDATE':
          console.log('[WS] BUTTON_UPDATE:', msg.payload)
          s.applyButtonUpdate(msg.payload as ButtonPayload)
          break
        case 'PROMPT_CHOICE':
        case 'PROMPT_CONFIRM':
        case 'PROMPT_AMOUNT':
          console.log('[WS] Prompt:', msg.type, msg.payload)
          s.setPrompt({
            type: msg.type,
            inputId: msg.inputId!,
            payload: msg.payload as PromptPayload,
          })
          break
        case 'GAME_LOG':
          s.addLogEntries(msg.payload as GameLogEntry[])
          break
        case 'GAME_OVER':
          s.clearButtons()
          s.setGameOver(msg.payload as { winner: string; message: string })
          break
        case 'ERROR':
          s.setError((msg.payload as { message: string }).message)
          break
        case 'MESSAGE':
        case 'SHOW_CARDS':
          // Log but no store update for v1
          break
      }
    }
  }

  private reconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      useGameStore.getState().setError('Could not reconnect to game.')
      return
    }

    const delay = this.reconnectTimeouts[this.reconnectAttempts] ?? 4000
    this.reconnectAttempts++

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }

  disconnect(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    // Set max attempts to prevent reconnect on intentional disconnect
    this.reconnectAttempts = this.maxReconnectAttempts
    if (this.ws) {
      // Detach handlers before closing to prevent the dead ws from
      // setting errors/reconnecting (StrictMode double-mount race)
      this.ws.onopen = null
      this.ws.onclose = null
      this.ws.onerror = null
      this.ws.onmessage = null
      this.ws.close()
      this.ws = null
    }
  }

  send(msg: InboundMessage): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(msg))
    }
  }

  sendButtonOk(): void {
    this.send({ type: 'BUTTON_OK', inputId: null, payload: null })
    useGameStore.getState().clearButtons()
  }

  sendButtonCancel(): void {
    this.send({ type: 'BUTTON_CANCEL', inputId: null, payload: null })
    useGameStore.getState().clearButtons()
  }

  sendChoiceResponse(inputId: string, indices: number[]): void {
    this.send({ type: 'CHOICE_RESPONSE', inputId, payload: indices })
  }

  sendConfirmResponse(inputId: string, confirmed: boolean): void {
    this.send({ type: 'CONFIRM_RESPONSE', inputId, payload: confirmed })
  }

  sendAmountResponse(inputId: string, amount: number): void {
    this.send({ type: 'AMOUNT_RESPONSE', inputId, payload: amount })
  }

  sendSelectCard(cardId: number): void {
    this.send({ type: 'SELECT_CARD', inputId: null, payload: cardId })
  }

  sendStartGame(config?: {
    deckName: string
    aiDeckName: string | null
    format: string
    aiDifficulty: string
  }): void {
    this.send({ type: 'START_GAME', inputId: null, payload: config ?? null })
  }
}
