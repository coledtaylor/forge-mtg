import { Tabs, TabsList, TabsTrigger, TabsContent } from '../ui/tabs'
import { StackPanel } from './StackPanel'
import { GameLogPanel } from './GameLogPanel'

interface RightPanelProps {
  className?: string
}

export function RightPanel({ className }: RightPanelProps) {
  return (
    <Tabs defaultValue="stack" className={`flex flex-col ${className ?? ''}`}>
      <TabsList className="shrink-0 w-full justify-start rounded-none border-b border-border bg-card px-2">
        <TabsTrigger value="stack" className="text-xs">Stack</TabsTrigger>
        <TabsTrigger value="log" className="text-xs">Log</TabsTrigger>
      </TabsList>
      <TabsContent value="stack" className="flex-1 overflow-hidden mt-0">
        <StackPanel className="h-full" />
      </TabsContent>
      <TabsContent value="log" className="flex-1 overflow-hidden mt-0">
        <GameLogPanel className="h-full" />
      </TabsContent>
    </Tabs>
  )
}
