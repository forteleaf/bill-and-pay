<script lang="ts">
  import { onMount } from "svelte";
  import {
    SvelteFlow,
    Controls,
    Background,
    MiniMap,
    Position,
    type Node,
    type Edge,
    type NodeTypes,
  } from "@xyflow/svelte";
  import "@xyflow/svelte/dist/style.css";
  import ELK from "elkjs/lib/elk.bundled.js";
  import type {
    Organization,
    OrgTree as OrgTreeType,
    CreateOrgRequest,
    OrgType,
  } from "@/types/api";
  import { apiClient } from "@/api/client";
  import { tabStore } from "@/stores/tab";
  import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
  } from "$lib/components/ui/card";
  import { Button } from "$lib/components/ui/button";
  import { Input } from "$lib/components/ui/input";
  import { Badge } from "$lib/components/ui/badge";
  import { Label } from "$lib/components/ui/label";
  import * as Select from "$lib/components/ui/select";
  import OrgFlowNode, {
    type OrgNodeData,
  } from "@/components/organization/OrgFlowNode.svelte";

  const NODE_WIDTH = 200;
  const NODE_HEIGHT = 80;

  const ORG_TYPE_ORDER = [
    "DISTRIBUTOR",
    "AGENCY",
    "DEALER",
    "SELLER",
    "VENDOR",
  ];

  const ORG_TYPE_LABELS: Record<string, string> = {
    DISTRIBUTOR: "총판",
    AGENCY: "대리점",
    DEALER: "딜러",
    SELLER: "셀러",
    VENDOR: "벤더",
  };

  const STATUS_LABELS: Record<string, string> = {
    ACTIVE: "정상",
    SUSPENDED: "정지",
    TERMINATED: "해지",
  };

  const ORG_TYPE_COLORS: Record<
    string,
    { bg: string; border: string; text: string; gradient: string }
  > = {
    DISTRIBUTOR: {
      bg: "bg-indigo-500/10",
      border: "border-indigo-500/40",
      text: "text-indigo-600",
      gradient: "from-indigo-500 to-indigo-600",
    },
    AGENCY: {
      bg: "bg-violet-500/10",
      border: "border-violet-500/40",
      text: "text-violet-600",
      gradient: "from-violet-500 to-violet-600",
    },
    DEALER: {
      bg: "bg-blue-500/10",
      border: "border-blue-500/40",
      text: "text-blue-600",
      gradient: "from-blue-500 to-blue-600",
    },
    SELLER: {
      bg: "bg-cyan-500/10",
      border: "border-cyan-500/40",
      text: "text-cyan-600",
      gradient: "from-cyan-500 to-cyan-600",
    },
    VENDOR: {
      bg: "bg-emerald-500/10",
      border: "border-emerald-500/40",
      text: "text-emerald-600",
      gradient: "from-emerald-500 to-emerald-600",
    },
  };

  const nodeTypes: NodeTypes = {
    org: OrgFlowNode,
  };

  let organizations = $state<OrgTreeType[]>([]);
  let flatOrganizations = $state<Organization[]>([]);
  let nodes = $state.raw<Node<OrgNodeData>[]>([]);
  let edges = $state.raw<Edge[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let selectedNodeId = $state<string | null>(null);

  let editingNodeId = $state<string | null>(null);
  let editName = $state("");
  let editStatus = $state<string>("ACTIVE");

  let showAddModal = $state(false);
  let addParentId = $state<string | null>(null);
  let newOrgName = $state("");
  let newOrgType = $state<string>("");

  let showMoveModal = $state(false);
  let moveNodeId = $state<string | null>(null);
  let moveTargetId = $state<string | null>(null);

  let toasts = $state<
    Array<{ id: number; message: string; type: "success" | "error" }>
  >([]);
  let toastCounter = 0;

  let layoutDirection = $state<"vertical" | "horizontal">("horizontal");

  let stats = $derived({
    total: countNodes(organizations),
    byType: countByType(organizations),
    byStatus: countByStatus(organizations),
  });

  let selectedNode = $derived(
    flatOrganizations.find((n) => n.id === selectedNodeId) || null,
  );
  let selectedNodeChildren = $derived.by(() => {
    if (!selectedNode) return [];
    return flatOrganizations.filter((org) => {
      const pathParts = org.path.split(".");
      const selectedPathParts = selectedNode.path.split(".");
      return (
        pathParts.length === selectedPathParts.length + 1 &&
        org.path.startsWith(selectedNode.path + ".")
      );
    });
  });

  let availableChildTypes = $derived.by(() => {
    if (!addParentId) return [];
    const parent = flatOrganizations.find((n) => n.id === addParentId);
    if (!parent) return [];
    const parentTypeIndex = ORG_TYPE_ORDER.indexOf(parent.orgType);
    return ORG_TYPE_ORDER.slice(parentTypeIndex + 1);
  });

  function countNodes(nodes: OrgTreeType[]): number {
    return nodes.reduce(
      (acc, node) => acc + 1 + (node.children ? countNodes(node.children) : 0),
      0,
    );
  }

  function countByType(nodes: OrgTreeType[]): Record<string, number> {
    const counts: Record<string, number> = {};
    function traverse(nodeList: OrgTreeType[]) {
      for (const node of nodeList) {
        counts[node.orgType] = (counts[node.orgType] || 0) + 1;
        if (node.children) traverse(node.children);
      }
    }
    traverse(nodes);
    return counts;
  }

  function countByStatus(nodes: OrgTreeType[]): Record<string, number> {
    const counts: Record<string, number> = {};
    function traverse(nodeList: OrgTreeType[]) {
      for (const node of nodeList) {
        counts[node.status] = (counts[node.status] || 0) + 1;
        if (node.children) traverse(node.children);
      }
    }
    traverse(nodes);
    return counts;
  }

  function buildTree(flatOrgs: Organization[]): OrgTreeType[] {
    const orgMap = new Map<string, OrgTreeType>();
    const roots: OrgTreeType[] = [];

    flatOrgs.forEach((org) => {
      orgMap.set(org.id, { ...org, children: [] });
    });

    flatOrgs.forEach((org) => {
      const node = orgMap.get(org.id)!;
      const pathSegments = org.path.split(".");

      if (pathSegments.length === 1) {
        roots.push(node);
      } else {
        const parentPath = pathSegments.slice(0, -1).join(".");
        const parent = flatOrgs.find((o) => o.path === parentPath);
        if (parent) {
          const parentNode = orgMap.get(parent.id);
          if (parentNode) parentNode.children.push(node);
        }
      }
    });

    function sortNodes(nodes: OrgTreeType[]): OrgTreeType[] {
      return nodes
        .sort((a, b) => {
          const aOrder = ORG_TYPE_ORDER.indexOf(a.orgType);
          const bOrder = ORG_TYPE_ORDER.indexOf(b.orgType);
          if (aOrder !== bOrder) return aOrder - bOrder;
          return a.name.localeCompare(b.name);
        })
        .map((node) => ({
          ...node,
          children: node.children ? sortNodes(node.children) : [],
        }));
    }

    return sortNodes(roots);
  }

  function organizationsToFlow(orgs: OrgTreeType[]): {
    nodes: Node<OrgNodeData>[];
    edges: Edge[];
  } {
    const flowNodes: Node<OrgNodeData>[] = [];
    const flowEdges: Edge[] = [];

    function traverse(org: OrgTreeType, parentId?: string) {
      const hasChildren = org.children && org.children.length > 0;

      flowNodes.push({
        id: org.id,
        type: "org",
        position: { x: 0, y: 0 },
        data: {
          name: org.name,
          orgCode: org.orgCode,
          orgType: org.orgType,
          status: org.status,
          path: org.path,
          level: org.level,
          hasChildren,
          merchantCount: org.merchantCount,
          layoutDirection,
          onAddChild: handleAddChild,
          onDoubleClick: handleNodeDoubleClick,
        },
      });

      if (parentId) {
        flowEdges.push({
          id: `e-${parentId}-${org.id}`,
          source: parentId,
          target: org.id,
          type: "smoothstep",
          animated: true,
          style:
            "stroke-width: 2px; stroke: hsl(var(--muted-foreground) / 0.5); stroke-dasharray: 5 5;",
        });
      }

      org.children?.forEach((child) => traverse(child, org.id));
    }

    orgs.forEach((org) => traverse(org));
    return { nodes: flowNodes, edges: flowEdges };
  }

  async function layoutNodes(
    inputNodes: Node<OrgNodeData>[],
    inputEdges: Edge[],
    direction: "vertical" | "horizontal" = "vertical",
  ): Promise<Node<OrgNodeData>[]> {
    try {
      const elk = new ELK();

      const graph = {
        id: "root",
        layoutOptions: {
          "elk.algorithm": "mrtree",
          "elk.direction": direction === "vertical" ? "DOWN" : "RIGHT",
          "spacing.nodeNode": "100",
          "spacing.edgeNode": "80",
          "elk.spacing.nodeNodeBetweenLayers": "120",
        },
        children: inputNodes.map((node) => ({
          id: node.id,
          width: NODE_WIDTH,
          height: NODE_HEIGHT,
        })),
        edges: inputEdges.map((edge) => ({
          id: `${edge.source}-${edge.target}`,
          sources: [edge.source],
          targets: [edge.target],
        })),
      };

      const layoutedGraph = await elk.layout(graph);

      const nodeMap = new Map(
        layoutedGraph.children?.map((node) => [node.id, node]) || [],
      );

      return inputNodes.map((node) => {
        const layoutedNode = nodeMap.get(node.id);
        return {
          ...node,
          position: {
            x: (layoutedNode?.x || 0) - NODE_WIDTH / 2,
            y: (layoutedNode?.y || 0) - NODE_HEIGHT / 2,
          },
          targetPosition:
            direction === "vertical" ? Position.Top : Position.Left,
          sourcePosition:
            direction === "vertical" ? Position.Bottom : Position.Right,
        };
      });
    } catch (e) {
      console.warn("ELK layout failed, using fallback:", e);
      // Fallback: return nodes with default positions
      return inputNodes.map((node, index) => ({
        ...node,
        position: { x: index * 250, y: 0 },
        targetPosition: direction === "vertical" ? Position.Top : Position.Left,
        sourcePosition:
          direction === "vertical" ? Position.Bottom : Position.Right,
      }));
    }
  }

  function showToast(message: string, type: "success" | "error") {
    const id = ++toastCounter;
    toasts = [...toasts, { id, message, type }];
    setTimeout(() => {
      toasts = toasts.filter((t) => t.id !== id);
    }, 3000);
  }

  async function loadOrganizations() {
    loading = true;
    error = null;
    try {
      const rootResponse = await apiClient.get<Organization>(
        "/organizations/root",
      );
      if (!rootResponse.success || !rootResponse.data) {
        error =
          rootResponse.error?.message || "Failed to load root organization";
        return;
      }

      const rootId = rootResponse.data.id;
      const response = await apiClient.get<Organization[]>(
        `/organizations/${rootId}/descendants`,
      );

      if (response.success && response.data) {
        flatOrganizations = response.data;
        organizations = buildTree(response.data);
        const { nodes: flowNodes, edges: flowEdges } =
          organizationsToFlow(organizations);
        nodes = await layoutNodes(flowNodes, flowEdges, layoutDirection);
        edges = flowEdges;
      } else {
        error = response.error?.message || "Failed to load organizations";
      }
    } catch (e) {
      error = e instanceof Error ? e.message : "Failed to load organizations";
    } finally {
      loading = false;
    }
  }

  async function updateOrganization(
    id: string,
    data: { name?: string; status?: string; parentId?: string },
  ) {
    try {
      const response = await apiClient.put<Organization>(
        `/organizations/${id}`,
        data,
      );
      if (response.success) {
        showToast("조직 정보가 수정되었습니다.", "success");
        await loadOrganizations();
      } else {
        showToast(response.error?.message || "수정 실패", "error");
      }
    } catch (e) {
      showToast(e instanceof Error ? e.message : "수정 실패", "error");
    }
  }

  async function createOrganization(data: CreateOrgRequest) {
    try {
      const response = await apiClient.post<Organization>(
        "/organizations",
        data,
      );
      if (response.success) {
        showToast("새 조직이 생성되었습니다.", "success");
        await loadOrganizations();
        return true;
      } else {
        showToast(response.error?.message || "생성 실패", "error");
        return false;
      }
    } catch (e) {
      showToast(e instanceof Error ? e.message : "생성 실패", "error");
      return false;
    }
  }

  function handleNodeClick({ node }: { node: Node<OrgNodeData> }) {
    selectedNodeId = node.id;
  }

  function handleNodeDoubleClick(nodeId: string) {
    const org = flatOrganizations.find((o) => o.id === nodeId);
    if (org) {
      editingNodeId = nodeId;
      editName = org.name;
      editStatus = org.status;
    }
  }

  function handleEditSave() {
    if (editingNodeId && editName.trim()) {
      updateOrganization(editingNodeId, {
        name: editName.trim(),
        status: editStatus,
      });
      editingNodeId = null;
    }
  }

  function handleEditCancel() {
    editingNodeId = null;
    editName = "";
    editStatus = "ACTIVE";
  }

  function handleAddChild(parentId: string) {
    addParentId = parentId;
    newOrgName = "";
    newOrgType = "";
    showAddModal = true;
  }

  async function handleAddChildSubmit() {
    if (!addParentId || !newOrgName.trim() || !newOrgType) return;

    const success = await createOrganization({
      name: newOrgName.trim(),
      orgType: newOrgType as OrgType,
      parentId: addParentId,
    });

    if (success) {
      showAddModal = false;
      addParentId = null;
      newOrgName = "";
      newOrgType = "";
    }
  }

  function handleOpenDetail(org: Organization) {
    tabStore.openTab({
      id: `branch-${org.id}`,
      title: org.name,
      icon: "🏢",
      component: "BranchDetail",
      closeable: true,
      props: { branchId: org.id },
    });
  }

  function handleNodeDragStop({
    targetNode,
  }: {
    targetNode: Node<OrgNodeData> | null;
  }) {
    if (!targetNode || targetNode.data.orgType === "DISTRIBUTOR") return;

    const draggedNodePos = targetNode.position;
    const draggedOrgType = targetNode.data.orgType;
    const draggedTypeIndex = ORG_TYPE_ORDER.indexOf(draggedOrgType);

    let closestNode: Node<OrgNodeData> | null = null;
    let closestDist = Infinity;

    for (const node of nodes) {
      if (node.id === targetNode.id) continue;

      const targetTypeIndex = ORG_TYPE_ORDER.indexOf(node.data.orgType);
      if (targetTypeIndex >= draggedTypeIndex) continue;

      if (
        node.data.path &&
        targetNode.data.path.startsWith(node.data.path + ".")
      ) {
        continue;
      }

      const dx = draggedNodePos.x - node.position.x;
      const dy = draggedNodePos.y - node.position.y;
      const dist = Math.sqrt(dx * dx + dy * dy);

      if (dist < closestDist && dist < 200) {
        closestDist = dist;
        closestNode = node;
      }
    }

    if (closestNode) {
      moveNodeId = targetNode.id;
      moveTargetId = closestNode.id;
      showMoveModal = true;
    }
    // Note: Removed unconditional loadOrganizations() to allow free node dragging
    // Layout reset only happens when move modal is cancelled (handleMoveCancel)
  }

  async function handleMoveConfirm() {
    if (moveNodeId && moveTargetId) {
      await updateOrganization(moveNodeId, { parentId: moveTargetId });
    }
    showMoveModal = false;
    moveNodeId = null;
    moveTargetId = null;
  }

  function handleMoveCancel() {
    showMoveModal = false;
    moveNodeId = null;
    moveTargetId = null;
    loadOrganizations();
  }

  async function toggleLayout() {
    layoutDirection =
      layoutDirection === "vertical" ? "horizontal" : "vertical";
    const { nodes: flowNodes, edges: flowEdges } =
      organizationsToFlow(organizations);
    nodes = await layoutNodes(flowNodes, flowEdges, layoutDirection);
  }

  onMount(() => {
    loadOrganizations();
  });
</script>

<div
  class="flex flex-col gap-6 p-6 max-w-[2000px] mx-auto h-[calc(100vh-80px)]"
>
  <header class="flex justify-between items-center shrink-0">
    <div class="flex items-baseline gap-4">
      <h1
        class="text-2xl font-bold text-foreground tracking-tight flex items-center gap-3"
      >
        <span
          class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white text-lg shadow-lg shadow-indigo-500/25"
        >
          🌐
        </span>
        조직 계층도
      </h1>
      <span
        class="text-sm text-muted-foreground font-medium bg-muted px-3 py-1 rounded-full"
      >
        총 {stats.total.toLocaleString()}개 조직
      </span>
    </div>
    <div class="flex items-center gap-3">
      <Button
        variant="outline"
        onclick={() => loadOrganizations()}
        disabled={loading}
      >
        <svg
          class="w-4 h-4 mr-2 {loading ? 'animate-spin' : ''}"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M21 12a9 9 0 11-9-9c2.52 0 4.93 1 6.74 2.74L21 8" />
          <path d="M21 3v5h-5" />
        </svg>
        새로고침
      </Button>
    </div>
  </header>

  <div class="flex items-center gap-3 overflow-x-auto pb-2 shrink-0">
    {#each ORG_TYPE_ORDER as type}
      {@const colors = ORG_TYPE_COLORS[type]}
      <div
        class="flex items-center gap-2 px-4 py-2 rounded-xl {colors.bg} border {colors.border} whitespace-nowrap"
      >
        <div
          class="w-6 h-6 rounded-lg bg-gradient-to-br {colors.gradient} flex items-center justify-center text-white text-xs font-bold shadow-sm"
        >
          {ORG_TYPE_LABELS[type].charAt(0)}
        </div>
        <span class="text-sm font-medium {colors.text}">
          {ORG_TYPE_LABELS[type]}
        </span>
        <span class="text-lg font-bold {colors.text}">
          {stats.byType[type] || 0}
        </span>
      </div>
    {/each}
    <div
      class="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/40 whitespace-nowrap"
    >
      <div class="w-2 h-2 rounded-full bg-emerald-500"></div>
      <span class="text-sm font-medium text-emerald-600">정상</span>
      <span class="text-lg font-bold text-emerald-600"
        >{stats.byStatus["ACTIVE"] || 0}</span
      >
    </div>
    <div class="flex-1"></div>
    <Button
      variant="outline"
      size="sm"
      onclick={toggleLayout}
      class="flex items-center gap-2 whitespace-nowrap"
    >
      {#if layoutDirection === "vertical"}
        <svg
          class="w-4 h-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M21 12H3M12 3v18M7 8l5-5 5 5M7 16l5 5 5-5" />
        </svg>
        가로 배치
      {:else}
        <svg
          class="w-4 h-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M12 21V3M3 12h18M8 7l-5 5 5 5M16 7l5 5-5 5" />
        </svg>
        세로 배치
      {/if}
    </Button>
  </div>

  <div class="flex-1 grid grid-cols-1 lg:grid-cols-4 gap-6 min-h-0">
    <div class="lg:col-span-3">
      <Card class="h-full">
        <CardContent class="p-0 h-full">
          {#if loading}
            <div
              class="flex flex-col items-center justify-center h-full gap-4 text-muted-foreground"
            >
              <div
                class="w-12 h-12 border-4 border-muted border-t-primary rounded-full animate-spin"
              ></div>
              <span class="font-medium">조직 정보를 불러오는 중...</span>
            </div>
          {:else if error}
            <div
              class="flex flex-col items-center justify-center h-full gap-4 text-destructive"
            >
              <svg
                class="w-16 h-16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
              >
                <circle cx="12" cy="12" r="10" />
                <path d="M12 8v4m0 4h.01" />
              </svg>
              <span class="font-medium text-lg">{error}</span>
              <Button variant="outline" onclick={() => loadOrganizations()}
                >다시 시도</Button
              >
            </div>
          {:else}
            <div
              class="h-full w-full"
              style="height: calc(100vh - 360px); min-height: 500px;"
            >
              <SvelteFlow
                {nodes}
                {edges}
                {nodeTypes}
                fitView
                nodesDraggable={true}
                nodesConnectable={false}
                onnodeclick={handleNodeClick}
                onnodedragstop={handleNodeDragStop}
                proOptions={{ hideAttribution: true }}
              >
                <Controls position="bottom-left" />
                <Background gap={20} />
                <MiniMap
                  nodeColor={(node) => {
                    const colors: Record<string, string> = {
                      DISTRIBUTOR: "#6366f1",
                      AGENCY: "#8b5cf6",
                      DEALER: "#3b82f6",
                      SELLER: "#06b6d4",
                      VENDOR: "#10b981",
                    };
                    return colors[node.data?.orgType as string] || "#94a3b8";
                  }}
                  class="!bg-slate-100 dark:!bg-slate-800 !border-slate-200 dark:!border-slate-700"
                />
              </SvelteFlow>
            </div>
          {/if}
        </CardContent>
      </Card>
    </div>

    <div class="lg:col-span-1">
      <Card class="h-full flex flex-col">
        <CardHeader class="pb-4 border-b border-border shrink-0">
          <CardTitle class="text-lg flex items-center gap-2">
            <svg
              class="w-5 h-5 text-muted-foreground"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5"
              />
            </svg>
            조직 상세
          </CardTitle>
        </CardHeader>
        <CardContent class="flex-1 overflow-y-auto p-4">
          {#if selectedNode}
            {@const colors = ORG_TYPE_COLORS[selectedNode.orgType]}
            <div class="flex flex-col gap-6">
              <div class="flex items-start gap-3">
                <div
                  class="w-12 h-12 rounded-xl bg-gradient-to-br {colors.gradient} flex items-center justify-center text-white font-bold text-lg shadow-lg"
                >
                  {ORG_TYPE_LABELS[selectedNode.orgType].charAt(0)}
                </div>
                <div class="flex-1 min-w-0">
                  <h3 class="font-bold text-lg text-foreground truncate">
                    {selectedNode.name}
                  </h3>
                  <p class="text-sm text-muted-foreground font-mono">
                    {selectedNode.orgCode}
                  </p>
                </div>
              </div>

              <div class="flex flex-wrap gap-2">
                <Badge class="{colors.bg} {colors.text} {colors.border} border">
                  {ORG_TYPE_LABELS[selectedNode.orgType]}
                </Badge>
                <Badge
                  variant={selectedNode.status === "ACTIVE"
                    ? "default"
                    : selectedNode.status === "SUSPENDED"
                      ? "secondary"
                      : "destructive"}
                >
                  {STATUS_LABELS[selectedNode.status]}
                </Badge>
              </div>

              <div class="grid grid-cols-2 gap-4 p-4 bg-muted/30 rounded-xl">
                <div>
                  <span
                    class="text-xs text-muted-foreground uppercase tracking-wide"
                    >계층 레벨</span
                  >
                  <p class="font-semibold text-foreground mt-1">
                    {selectedNode.level}단계
                  </p>
                </div>
                <div>
                  <span
                    class="text-xs text-muted-foreground uppercase tracking-wide"
                    >하위 조직</span
                  >
                  <p class="font-semibold text-foreground mt-1">
                    {selectedNodeChildren.length}개
                  </p>
                </div>
                <div class="col-span-2">
                  <span
                    class="text-xs text-muted-foreground uppercase tracking-wide"
                    >경로</span
                  >
                  <p class="font-mono text-sm text-foreground mt-1 break-all">
                    {selectedNode.path}
                  </p>
                </div>
              </div>

              {#if selectedNodeChildren.length > 0}
                <div>
                  <span
                    class="text-sm font-medium text-muted-foreground mb-2 block"
                    >하위 조직</span
                  >
                  <div class="flex flex-wrap gap-1.5 max-h-32 overflow-y-auto">
                    {#each selectedNodeChildren.slice(0, 15) as child}
                      {@const childColors = ORG_TYPE_COLORS[child.orgType]}
                      <button
                        onclick={() => (selectedNodeId = child.id)}
                        class="px-2 py-1 text-xs rounded-lg border transition-colors hover:bg-muted {childColors.bg} {childColors.border} {childColors.text}"
                      >
                        {child.name}
                      </button>
                    {/each}
                    {#if selectedNodeChildren.length > 15}
                      <span class="px-2 py-1 text-xs text-muted-foreground">
                        +{selectedNodeChildren.length - 15} more
                      </span>
                    {/if}
                  </div>
                </div>
              {/if}

              <div class="flex flex-col gap-2 pt-4 border-t border-border">
                <Button
                  class="w-full"
                  onclick={() => selectedNode && handleOpenDetail(selectedNode)}
                >
                  <svg
                    class="w-4 h-4 mr-2"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path
                      d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                    />
                  </svg>
                  상세 정보 보기
                </Button>
                <Button
                  variant="outline"
                  class="w-full"
                  onclick={() =>
                    selectedNode && handleNodeDoubleClick(selectedNode.id)}
                >
                  <svg
                    class="w-4 h-4 mr-2"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"
                    />
                    <path
                      d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"
                    />
                  </svg>
                  편집
                </Button>
                {#if ORG_TYPE_ORDER.indexOf(selectedNode.orgType) < ORG_TYPE_ORDER.length - 1}
                  <Button
                    variant="secondary"
                    class="w-full"
                    onclick={() =>
                      selectedNode && handleAddChild(selectedNode.id)}
                  >
                    <svg
                      class="w-4 h-4 mr-2"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path d="M12 5v14m-7-7h14" />
                    </svg>
                    하위 조직 추가
                  </Button>
                {/if}
              </div>
            </div>
          {:else}
            <div
              class="flex flex-col items-center justify-center h-full gap-4 text-muted-foreground"
            >
              <div
                class="w-20 h-20 rounded-2xl bg-muted/50 flex items-center justify-center"
              >
                <svg
                  class="w-10 h-10 opacity-50"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                >
                  <path
                    d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122"
                  />
                </svg>
              </div>
              <div class="text-center">
                <p class="font-medium">조직을 선택하세요</p>
                <p class="text-sm mt-1">
                  조직도에서 노드를 클릭하면<br />상세 정보가 표시됩니다
                </p>
              </div>
            </div>
          {/if}
        </CardContent>
      </Card>
    </div>
  </div>
</div>

{#if showAddModal}
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
  >
    <div
      class="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden"
    >
      <div class="px-6 py-4 border-b border-border bg-muted/30">
        <h3 class="text-lg font-bold text-foreground">하위 조직 추가</h3>
        <p class="text-sm text-muted-foreground mt-1">
          새로운 하위 조직을 생성합니다
        </p>
      </div>
      <div class="p-6 flex flex-col gap-4">
        <div>
          <Label for="org-name" class="text-sm font-medium">조직명</Label>
          <Input
            id="org-name"
            value={newOrgName}
            oninput={(e) => (newOrgName = e.currentTarget.value)}
            placeholder="조직명을 입력하세요"
            class="mt-1.5"
          />
        </div>
        <div>
          <Label for="org-type" class="text-sm font-medium">조직 유형</Label>
          <Select.Root type="single" value={newOrgType || undefined} onValueChange={(v) => newOrgType = v || ''}>
            <Select.Trigger id="org-type" class="mt-1.5 w-full">
              {#if newOrgType}
                {ORG_TYPE_LABELS[newOrgType]}
              {:else}
                <span class="text-muted-foreground">유형 선택</span>
              {/if}
            </Select.Trigger>
            <Select.Content>
              {#each availableChildTypes as type}
                <Select.Item value={type} label={ORG_TYPE_LABELS[type]}>
                  {ORG_TYPE_LABELS[type]}
                </Select.Item>
              {/each}
            </Select.Content>
          </Select.Root>
        </div>
      </div>
      <div
        class="px-6 py-4 border-t border-border bg-muted/30 flex justify-end gap-3"
      >
        <Button variant="outline" onclick={() => (showAddModal = false)}>
          취소
        </Button>
        <Button
          onclick={handleAddChildSubmit}
          disabled={!newOrgName.trim() || !newOrgType}
        >
          생성
        </Button>
      </div>
    </div>
  </div>
{/if}

{#if editingNodeId}
  {@const editingOrg = flatOrganizations.find((o) => o.id === editingNodeId)}
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
  >
    <div
      class="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden"
    >
      <div class="px-6 py-4 border-b border-border bg-muted/30">
        <h3 class="text-lg font-bold text-foreground">조직 정보 편집</h3>
        <p class="text-sm text-muted-foreground mt-1">
          {editingOrg?.name || ""} 정보를 수정합니다
        </p>
      </div>
      <div class="p-6 flex flex-col gap-4">
        <div>
          <Label for="edit-name" class="text-sm font-medium">조직명</Label>
          <Input
            id="edit-name"
            value={editName}
            oninput={(e) => (editName = e.currentTarget.value)}
            placeholder="조직명을 입력하세요"
            class="mt-1.5"
          />
        </div>
        <div>
          <Label for="edit-status" class="text-sm font-medium">상태</Label>
          <Select.Root type="single" value={editStatus} onValueChange={(v) => editStatus = v || 'ACTIVE'}>
            <Select.Trigger id="edit-status" class="mt-1.5 w-full">
              {STATUS_LABELS[editStatus] || editStatus}
            </Select.Trigger>
            <Select.Content>
              <Select.Item value="ACTIVE" label="정상">정상</Select.Item>
              <Select.Item value="SUSPENDED" label="정지">정지</Select.Item>
              <Select.Item value="TERMINATED" label="해지">해지</Select.Item>
            </Select.Content>
          </Select.Root>
        </div>
      </div>
      <div
        class="px-6 py-4 border-t border-border bg-muted/30 flex justify-end gap-3"
      >
        <Button variant="outline" onclick={handleEditCancel}>취소</Button>
        <Button onclick={handleEditSave} disabled={!editName.trim()}>
          저장
        </Button>
      </div>
    </div>
  </div>
{/if}

{#if showMoveModal}
  {@const moveNode = flatOrganizations.find((n) => n.id === moveNodeId)}
  {@const targetNode = flatOrganizations.find((n) => n.id === moveTargetId)}
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
  >
    <div
      class="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden"
    >
      <div class="px-6 py-4 border-b border-border bg-amber-500/10">
        <h3 class="text-lg font-bold text-foreground flex items-center gap-2">
          <svg
            class="w-5 h-5 text-amber-500"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
          조직 이동 확인
        </h3>
      </div>
      <div class="p-6">
        <p class="text-foreground">
          <strong class="text-primary">{moveNode?.name}</strong>을(를)
          <strong class="text-primary">{targetNode?.name}</strong> 아래로 이동하시겠습니까?
        </p>
        <p class="text-sm text-muted-foreground mt-2">
          이 작업은 조직의 계층 구조를 변경합니다.
        </p>
      </div>
      <div
        class="px-6 py-4 border-t border-border bg-muted/30 flex justify-end gap-3"
      >
        <Button variant="outline" onclick={handleMoveCancel}>취소</Button>
        <Button onclick={handleMoveConfirm}>이동</Button>
      </div>
    </div>
  </div>
{/if}

<div class="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
  {#each toasts as toast (toast.id)}
    <div
      class="px-4 py-3 rounded-xl shadow-lg backdrop-blur-sm flex items-center gap-3 animate-in slide-in-from-right duration-300
        {toast.type === 'success'
        ? 'bg-emerald-500/90 text-white'
        : 'bg-rose-500/90 text-white'}"
    >
      {#if toast.type === "success"}
        <svg
          class="w-5 h-5"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M20 6L9 17l-5-5" />
        </svg>
      {:else}
        <svg
          class="w-5 h-5"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <circle cx="12" cy="12" r="10" />
          <path d="M12 8v4m0 4h.01" />
        </svg>
      {/if}
      <span class="font-medium">{toast.message}</span>
    </div>
  {/each}
</div>
