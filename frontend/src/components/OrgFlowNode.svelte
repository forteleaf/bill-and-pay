<script module lang="ts">
  import type { Node } from "@xyflow/svelte";

  export interface OrgNodeData {
    name: string;
    orgCode: string;
    orgType: string;
    status: string;
    path: string;
    level: number;
    hasChildren: boolean;
    layoutDirection?: "vertical" | "horizontal";
    onAddChild?: (nodeId: string) => void;
    onDoubleClick?: (nodeId: string) => void;
    [key: string]: unknown;
  }

  export type OrgNodeType = Node<OrgNodeData, "org">;
</script>

<script lang="ts">
  import { Handle, Position, type NodeProps } from "@xyflow/svelte";
  import { Badge } from "$lib/components/ui/badge";

  let { id, data }: NodeProps<OrgNodeType> = $props();

  let isHovered = $state(false);
  let hoverTimeout: ReturnType<typeof setTimeout> | null = null;

  // Layout direction determines handle positions
  let isHorizontal = $derived(data.layoutDirection === "horizontal");
  let targetPosition = $derived(isHorizontal ? Position.Left : Position.Top);
  let sourcePosition = $derived(
    isHorizontal ? Position.Right : Position.Bottom,
  );

  function handleMouseEnter() {
    if (hoverTimeout) clearTimeout(hoverTimeout);
    isHovered = true;
  }

  function handleMouseLeave() {
    hoverTimeout = setTimeout(() => {
      isHovered = false;
    }, 300);
  }

  const ORG_TYPE_LABELS: Record<string, string> = {
    DISTRIBUTOR: "총판",
    AGENCY: "대리점",
    DEALER: "딜러",
    SELLER: "셀러",
    VENDOR: "가맹점",
  };

  const STATUS_LABELS: Record<string, string> = {
    ACTIVE: "정상",
    SUSPENDED: "정지",
    TERMINATED: "해지",
  };

  const ORG_TYPE_COLORS: Record<
    string,
    { bg: string; border: string; text: string; gradient: string; ring: string }
  > = {
    DISTRIBUTOR: {
      bg: "bg-indigo-500/10",
      border: "border-indigo-400",
      text: "text-indigo-600",
      gradient: "from-indigo-500 to-indigo-600",
      ring: "ring-indigo-500/30",
    },
    AGENCY: {
      bg: "bg-violet-500/10",
      border: "border-violet-400",
      text: "text-violet-600",
      gradient: "from-violet-500 to-violet-600",
      ring: "ring-violet-500/30",
    },
    DEALER: {
      bg: "bg-blue-500/10",
      border: "border-blue-400",
      text: "text-blue-600",
      gradient: "from-blue-500 to-blue-600",
      ring: "ring-blue-500/30",
    },
    SELLER: {
      bg: "bg-cyan-500/10",
      border: "border-cyan-400",
      text: "text-cyan-600",
      gradient: "from-cyan-500 to-cyan-600",
      ring: "ring-cyan-500/30",
    },
    VENDOR: {
      bg: "bg-emerald-500/10",
      border: "border-emerald-400",
      text: "text-emerald-600",
      gradient: "from-emerald-500 to-emerald-600",
      ring: "ring-emerald-500/30",
    },
  };

  const ORG_TYPE_ORDER = [
    "DISTRIBUTOR",
    "AGENCY",
    "DEALER",
    "SELLER",
    "VENDOR",
  ];

  let colors = $derived(
    ORG_TYPE_COLORS[data.orgType] || ORG_TYPE_COLORS.DISTRIBUTOR,
  );
  let canAddChild = $derived(
    ORG_TYPE_ORDER.indexOf(data.orgType) < ORG_TYPE_ORDER.length - 1,
  );

  function handleDoubleClick(e: MouseEvent) {
    e.stopPropagation();
    data.onDoubleClick?.(id);
  }

  function handleAddChild(e: MouseEvent) {
    e.stopPropagation();
    data.onAddChild?.(id);
  }
</script>

<div
  class="relative group"
  onmouseenter={handleMouseEnter}
  onmouseleave={handleMouseLeave}
>
  <Handle
    type="target"
    position={targetPosition}
    class="!w-3 !h-3 !bg-slate-400 !border-2 !border-white dark:!border-slate-800 !rounded-full {isHorizontal
      ? '!-left-1.5'
      : '!-top-1.5'}"
  />

  <div
    class="w-[200px] bg-white dark:bg-slate-800 rounded-xl border-2 {colors.border} shadow-lg transition-all duration-200 hover:shadow-xl hover:ring-4 {colors.ring} cursor-pointer overflow-hidden"
    ondblclick={handleDoubleClick}
  >
    <div class="h-1.5 bg-gradient-to-r {colors.gradient}"></div>

    <div class="px-4 py-3">
      <div class="flex items-center gap-2 mb-2">
        <div
          class="w-8 h-8 rounded-lg bg-gradient-to-br {colors.gradient} flex items-center justify-center text-white text-sm font-bold shadow-sm shrink-0"
        >
          {ORG_TYPE_LABELS[data.orgType]?.charAt(0) || "O"}
        </div>
        <div class="flex-1 min-w-0">
          <div
            class="font-semibold text-sm text-foreground truncate leading-tight"
          >
            {data.name}
          </div>
          <div class="text-xs text-muted-foreground font-mono truncate">
            {data.path.split(".").pop()}
          </div>
        </div>
      </div>

      <div class="flex items-center justify-between gap-2">
        <span
          class="text-xs {colors.text} font-medium px-2 py-0.5 rounded-md {colors.bg}"
        >
          {ORG_TYPE_LABELS[data.orgType]}
        </span>
        {#if data.status === "ACTIVE"}
          <Badge
            variant="default"
            class="text-xs px-1.5 py-0 h-5 bg-emerald-500 hover:bg-emerald-500"
          >
            {STATUS_LABELS[data.status]}
          </Badge>
        {:else if data.status === "SUSPENDED"}
          <Badge
            variant="secondary"
            class="text-xs px-1.5 py-0 h-5 bg-amber-500 text-white hover:bg-amber-500"
          >
            {STATUS_LABELS[data.status]}
          </Badge>
        {:else}
          <Badge variant="destructive" class="text-xs px-1.5 py-0 h-5">
            {STATUS_LABELS[data.status]}
          </Badge>
        {/if}
      </div>
    </div>
  </div>

  <Handle
    type="source"
    position={sourcePosition}
    class="!w-3 !h-3 !bg-slate-400 !border-2 !border-white dark:!border-slate-800 !rounded-full {isHorizontal
      ? '!-right-1.5'
      : '!-bottom-1.5'}"
  />

  {#if canAddChild && isHovered}
    <button
      onclick={handleAddChild}
      onmouseenter={handleMouseEnter}
      class="absolute {isHorizontal
        ? '-right-10 top-1/2 -translate-y-1/2'
        : '-bottom-10 left-1/2 -translate-x-1/2'} w-8 h-8 rounded-full bg-primary/90 hover:bg-primary text-white shadow-lg flex items-center justify-center transition-all duration-200 hover:scale-110 z-10"
      title="하위 조직 추가"
    >
      <svg
        class="w-5 h-5"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.5"
      >
        <path d="M12 5v14m-7-7h14" />
      </svg>
    </button>
  {/if}
</div>
