<script lang="ts">
  import { onMount } from 'svelte';
  import type { Organization, OrgTree as OrgTreeType, CreateOrgRequest, OrgType } from '../../types/api';
  import { apiClient } from '../../lib/api';
  import { tabStore } from '../../lib/tabStore';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Badge } from '$lib/components/ui/badge';
  import { Label } from '$lib/components/ui/label';
  
  // ═══════════════════════════════════════════════════════════════════════════════
  // TYPES & CONSTANTS
  // ═══════════════════════════════════════════════════════════════════════════════
  
  interface MindmapNode extends OrgTreeType {
    x: number;
    y: number;
    width: number;
    height: number;
    collapsed: boolean;
  }
  
  interface DragState {
    isDragging: boolean;
    nodeId: string | null;
    startX: number;
    startY: number;
    offsetX: number;
    offsetY: number;
  }
  
  interface PanState {
    isPanning: boolean;
    startX: number;
    startY: number;
  }
  
  const ORG_TYPE_ORDER = ['DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR'];
  
  const ORG_TYPE_LABELS: Record<string, string> = {
    DISTRIBUTOR: '총판',
    AGENCY: '대리점',
    DEALER: '딜러',
    SELLER: '판매점',
    VENDOR: '가맹점'
  };
  
  const STATUS_LABELS: Record<string, string> = {
    ACTIVE: '정상',
    SUSPENDED: '정지',
    TERMINATED: '해지'
  };
  
  const ORG_TYPE_COLORS: Record<string, { bg: string; border: string; text: string; gradient: string }> = {
    DISTRIBUTOR: { bg: 'bg-indigo-500/10', border: 'border-indigo-500/40', text: 'text-indigo-600', gradient: 'from-indigo-500 to-indigo-600' },
    AGENCY: { bg: 'bg-violet-500/10', border: 'border-violet-500/40', text: 'text-violet-600', gradient: 'from-violet-500 to-violet-600' },
    DEALER: { bg: 'bg-blue-500/10', border: 'border-blue-500/40', text: 'text-blue-600', gradient: 'from-blue-500 to-blue-600' },
    SELLER: { bg: 'bg-cyan-500/10', border: 'border-cyan-500/40', text: 'text-cyan-600', gradient: 'from-cyan-500 to-cyan-600' },
    VENDOR: { bg: 'bg-emerald-500/10', border: 'border-emerald-500/40', text: 'text-emerald-600', gradient: 'from-emerald-500 to-emerald-600' }
  };

  const STATUS_COLORS: Record<string, string> = {
    ACTIVE: 'fill-emerald-500',
    SUSPENDED: 'fill-amber-500',
    TERMINATED: 'fill-rose-500'
  };
  
  // Layout constants
  const NODE_WIDTH = 180;
  const NODE_HEIGHT = 64;
  const HORIZONTAL_GAP = 80;
  const VERTICAL_GAP = 20;
  const INITIAL_X = 60;
  const INITIAL_Y = 60;

  // ═══════════════════════════════════════════════════════════════════════════════
  // STATE
  // ═══════════════════════════════════════════════════════════════════════════════
  
  let organizations = $state<OrgTreeType[]>([]);
  let mindmapNodes = $state<MindmapNode[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let selectedNodeId = $state<string | null>(null);
  let hoveredNodeId = $state<string | null>(null);
  
  // Edit state
  let editingNodeId = $state<string | null>(null);
  let editName = $state('');
  let editStatus = $state<string>('ACTIVE');
  
  // Add child modal state
  let showAddModal = $state(false);
  let addParentId = $state<string | null>(null);
  let newOrgName = $state('');
  let newOrgType = $state<string>('');
  
  // Move confirmation modal
  let showMoveModal = $state(false);
  let moveNodeId = $state<string | null>(null);
  let moveTargetId = $state<string | null>(null);
  
  // Zoom & Pan
  let scale = $state(1);
  let translateX = $state(0);
  let translateY = $state(0);
  let panState = $state<PanState>({ isPanning: false, startX: 0, startY: 0 });
  
  // Drag & Drop
  let dragState = $state<DragState>({
    isDragging: false,
    nodeId: null,
    startX: 0,
    startY: 0,
    offsetX: 0,
    offsetY: 0
  });
  let dropTargetId = $state<string | null>(null);
  
  // SVG container ref
  let svgContainer: SVGSVGElement | null = null;
  
  // Toast notifications
  let toasts = $state<Array<{ id: number; message: string; type: 'success' | 'error' }>>([]);
  let toastCounter = 0;

  // ═══════════════════════════════════════════════════════════════════════════════
  // DERIVED VALUES
  // ═══════════════════════════════════════════════════════════════════════════════
  
  let stats = $derived({
    total: countNodes(organizations),
    byType: countByType(organizations),
    byStatus: countByStatus(organizations)
  });
  
  let selectedNode = $derived(mindmapNodes.find(n => n.id === selectedNodeId) || null);
  
  let visibleNodes = $derived(getVisibleNodes(mindmapNodes));
  
  let connections = $derived(generateConnections(visibleNodes));
  
  let svgDimensions = $derived(calculateSvgDimensions(visibleNodes));
  
  // Next available org type for adding children
  let availableChildTypes = $derived.by(() => {
    if (!addParentId) return [];
    const parent = mindmapNodes.find(n => n.id === addParentId);
    if (!parent) return [];
    const parentTypeIndex = ORG_TYPE_ORDER.indexOf(parent.orgType);
    return ORG_TYPE_ORDER.slice(parentTypeIndex + 1);
  });

  // ═══════════════════════════════════════════════════════════════════════════════
  // HELPER FUNCTIONS
  // ═══════════════════════════════════════════════════════════════════════════════
  
  function countNodes(nodes: OrgTreeType[]): number {
    return nodes.reduce((acc, node) => acc + 1 + (node.children ? countNodes(node.children) : 0), 0);
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
    
    flatOrgs.forEach(org => {
      orgMap.set(org.id, { ...org, children: [] });
    });
    
    flatOrgs.forEach(org => {
      const node = orgMap.get(org.id)!;
      const pathSegments = org.path.split('.');
      
      if (pathSegments.length === 1) {
        roots.push(node);
      } else {
        const parentPath = pathSegments.slice(0, -1).join('.');
        const parent = flatOrgs.find(o => o.path === parentPath);
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
        .map(node => ({ ...node, children: node.children ? sortNodes(node.children) : [] }));
    }
    
    return sortNodes(roots);
  }
  
  function treeToMindmap(
    nodes: OrgTreeType[],
    parentX: number = INITIAL_X,
    startY: number = INITIAL_Y,
    collapsedSet: Set<string> = new Set()
  ): { nodes: MindmapNode[]; totalHeight: number } {
    const result: MindmapNode[] = [];
    let currentY = startY;
    
    for (const node of nodes) {
      const collapsed = collapsedSet.has(node.id);
      const x = parentX;
      const y = currentY;
      
      const mindmapNode: MindmapNode = {
        ...node,
        x,
        y,
        width: NODE_WIDTH,
        height: NODE_HEIGHT,
        collapsed
      };
      
      result.push(mindmapNode);
      
      let subtreeHeight = NODE_HEIGHT;
      
      if (node.children && node.children.length > 0 && !collapsed) {
        const childResult = treeToMindmap(
          node.children,
          x + NODE_WIDTH + HORIZONTAL_GAP,
          currentY,
          collapsedSet
        );
        result.push(...childResult.nodes);
        subtreeHeight = Math.max(subtreeHeight, childResult.totalHeight);
      }
      
      currentY += subtreeHeight + VERTICAL_GAP;
    }
    
    return { nodes: result, totalHeight: currentY - startY - VERTICAL_GAP };
  }
  
  function getCollapsedSet(): Set<string> {
    const set = new Set<string>();
    for (const node of mindmapNodes) {
      if (node.collapsed) set.add(node.id);
    }
    return set;
  }
  
  function recalculateLayout() {
    const collapsedSet = getCollapsedSet();
    const { nodes } = treeToMindmap(organizations, INITIAL_X, INITIAL_Y, collapsedSet);
    mindmapNodes = nodes;
  }
  
  function getVisibleNodes(nodes: MindmapNode[]): MindmapNode[] {
    const collapsedIds = new Set(nodes.filter(n => n.collapsed).map(n => n.id));
    const hiddenIds = new Set<string>();
    
    for (const node of nodes) {
      const pathParts = node.path.split('.');
      for (let i = 1; i < pathParts.length; i++) {
        const ancestorPath = pathParts.slice(0, i).join('.');
        const ancestor = nodes.find(n => n.path === ancestorPath);
        if (ancestor && collapsedIds.has(ancestor.id)) {
          hiddenIds.add(node.id);
          break;
        }
      }
    }
    
    return nodes.filter(n => !hiddenIds.has(n.id));
  }
  
  function generateConnections(nodes: MindmapNode[]): Array<{ from: MindmapNode; to: MindmapNode; path: string }> {
    const connections: Array<{ from: MindmapNode; to: MindmapNode; path: string }> = [];
    const nodeMap = new Map(nodes.map(n => [n.path, n]));
    
    for (const node of nodes) {
      const pathParts = node.path.split('.');
      if (pathParts.length > 1) {
        const parentPath = pathParts.slice(0, -1).join('.');
        const parent = nodeMap.get(parentPath);
        if (parent) {
          // Create smooth bezier curve
          const startX = parent.x + parent.width;
          const startY = parent.y + parent.height / 2;
          const endX = node.x;
          const endY = node.y + node.height / 2;
          const controlX1 = startX + HORIZONTAL_GAP / 2;
          const controlX2 = endX - HORIZONTAL_GAP / 2;
          
          const path = `M ${startX} ${startY} C ${controlX1} ${startY}, ${controlX2} ${endY}, ${endX} ${endY}`;
          connections.push({ from: parent, to: node, path });
        }
      }
    }
    
    return connections;
  }
  
  function calculateSvgDimensions(nodes: MindmapNode[]): { width: number; height: number } {
    if (nodes.length === 0) return { width: 800, height: 600 };
    
    let maxX = 0;
    let maxY = 0;
    
    for (const node of nodes) {
      maxX = Math.max(maxX, node.x + node.width);
      maxY = Math.max(maxY, node.y + node.height);
    }
    
    return { width: maxX + 100, height: maxY + 100 };
  }
  
  function showToast(message: string, type: 'success' | 'error') {
    const id = ++toastCounter;
    toasts = [...toasts, { id, message, type }];
    setTimeout(() => {
      toasts = toasts.filter(t => t.id !== id);
    }, 3000);
  }

  // ═══════════════════════════════════════════════════════════════════════════════
  // API FUNCTIONS
  // ═══════════════════════════════════════════════════════════════════════════════
  
  async function loadOrganizations() {
    loading = true;
    error = null;
    try {
      const rootResponse = await apiClient.get<Organization>('/organizations/root');
      if (!rootResponse.success || !rootResponse.data) {
        error = rootResponse.error?.message || 'Failed to load root organization';
        return;
      }
      
      const rootId = rootResponse.data.id;
      const response = await apiClient.get<Organization[]>(`/organizations/${rootId}/descendants`);
      
      if (response.success && response.data) {
        organizations = buildTree(response.data);
        const { nodes } = treeToMindmap(organizations);
        mindmapNodes = nodes;
      } else {
        error = response.error?.message || 'Failed to load organizations';
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load organizations';
    } finally {
      loading = false;
    }
  }
  
  async function updateOrganization(id: string, data: { name?: string; status?: string }) {
    try {
      const response = await apiClient.put<Organization>(`/organizations/${id}`, data);
      if (response.success) {
        showToast('조직 정보가 수정되었습니다.', 'success');
        await loadOrganizations();
      } else {
        showToast(response.error?.message || '수정 실패', 'error');
      }
    } catch (e) {
      showToast(e instanceof Error ? e.message : '수정 실패', 'error');
    }
  }
  
  async function createOrganization(data: CreateOrgRequest) {
    try {
      const response = await apiClient.post<Organization>('/organizations', data);
      if (response.success) {
        showToast('새 조직이 생성되었습니다.', 'success');
        await loadOrganizations();
        return true;
      } else {
        showToast(response.error?.message || '생성 실패', 'error');
        return false;
      }
    } catch (e) {
      showToast(e instanceof Error ? e.message : '생성 실패', 'error');
      return false;
    }
  }
  
  async function moveOrganization(nodeId: string, newParentId: string) {
    try {
      const response = await apiClient.put<Organization>(`/organizations/${nodeId}`, {
        parentId: newParentId
      });
      if (response.success) {
        showToast('조직이 이동되었습니다.', 'success');
        await loadOrganizations();
        return true;
      } else {
        showToast(response.error?.message || '이동 실패', 'error');
        return false;
      }
    } catch (e) {
      showToast(e instanceof Error ? e.message : '이동 실패', 'error');
      return false;
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════════
  // EVENT HANDLERS
  // ═══════════════════════════════════════════════════════════════════════════════
  
  function handleNodeClick(node: MindmapNode) {
    if (dragState.isDragging) return;
    selectedNodeId = node.id;
  }
  
  function handleNodeDoubleClick(node: MindmapNode) {
    editingNodeId = node.id;
    editName = node.name;
    editStatus = node.status;
  }
  
  function handleEditSave() {
    if (editingNodeId && editName.trim()) {
      updateOrganization(editingNodeId, { name: editName.trim(), status: editStatus });
      editingNodeId = null;
    }
  }
  
  function handleEditCancel() {
    editingNodeId = null;
    editName = '';
    editStatus = 'ACTIVE';
  }
  
  function handleEditKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter') {
      handleEditSave();
    } else if (e.key === 'Escape') {
      handleEditCancel();
    }
  }
  
  function handleToggleCollapse(nodeId: string, e: MouseEvent) {
    e.stopPropagation();
    const node = mindmapNodes.find(n => n.id === nodeId);
    if (node) {
      node.collapsed = !node.collapsed;
      recalculateLayout();
    }
  }
  
  function handleAddChild(parentId: string) {
    addParentId = parentId;
    newOrgName = '';
    newOrgType = '';
    showAddModal = true;
  }
  
  async function handleAddChildSubmit() {
    if (!addParentId || !newOrgName.trim() || !newOrgType) return;
    
    const success = await createOrganization({
      name: newOrgName.trim(),
      orgType: newOrgType as OrgType,
      parentId: addParentId
    });
    
    if (success) {
      showAddModal = false;
      addParentId = null;
      newOrgName = '';
      newOrgType = '';
    }
  }
  
  function handleOpenDetail(org: MindmapNode) {
    tabStore.openTab({
      id: `branch-${org.id}`,
      title: org.name,
      icon: '🏢',
      component: 'BranchDetail',
      closeable: true,
      props: { branchId: org.id }
    });
  }
  
  // Drag & Drop handlers
  function handleDragStart(nodeId: string, e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    
    const node = mindmapNodes.find(n => n.id === nodeId);
    if (!node || node.orgType === 'DISTRIBUTOR') return; // Can't drag root
    
    dragState = {
      isDragging: true,
      nodeId,
      startX: e.clientX,
      startY: e.clientY,
      offsetX: 0,
      offsetY: 0
    };
  }
  
  function handleDragMove(e: MouseEvent) {
    if (!dragState.isDragging) return;
    
    dragState.offsetX = (e.clientX - dragState.startX) / scale;
    dragState.offsetY = (e.clientY - dragState.startY) / scale;
    
    // Find potential drop target
    const dragNode = mindmapNodes.find(n => n.id === dragState.nodeId);
    if (!dragNode) return;
    
    const dragX = dragNode.x + dragState.offsetX + NODE_WIDTH / 2;
    const dragY = dragNode.y + dragState.offsetY + NODE_HEIGHT / 2;
    
    // Find closest valid drop target
    let closestNode: MindmapNode | null = null;
    let closestDist = Infinity;
    
    for (const node of visibleNodes) {
      if (node.id === dragState.nodeId) continue;
      
      // Check if this is a valid parent (higher in hierarchy)
      const dragTypeIndex = ORG_TYPE_ORDER.indexOf(dragNode.orgType);
      const targetTypeIndex = ORG_TYPE_ORDER.indexOf(node.orgType);
      
      if (targetTypeIndex >= dragTypeIndex) continue; // Can only move to higher level
      
      // Check if not descendant of drag node
      if (node.orgCode.startsWith(dragNode.orgCode + '.')) continue;
      
      const dist = Math.sqrt(
        Math.pow(dragX - (node.x + NODE_WIDTH / 2), 2) +
        Math.pow(dragY - (node.y + NODE_HEIGHT / 2), 2)
      );
      
      if (dist < closestDist && dist < 150) {
        closestDist = dist;
        closestNode = node;
      }
    }
    
    dropTargetId = closestNode?.id || null;
  }
  
  function handleDragEnd() {
    if (dragState.isDragging && dragState.nodeId && dropTargetId) {
      moveNodeId = dragState.nodeId;
      moveTargetId = dropTargetId;
      showMoveModal = true;
    }
    
    dragState = {
      isDragging: false,
      nodeId: null,
      startX: 0,
      startY: 0,
      offsetX: 0,
      offsetY: 0
    };
    dropTargetId = null;
  }
  
  async function handleMoveConfirm() {
    if (moveNodeId && moveTargetId) {
      await moveOrganization(moveNodeId, moveTargetId);
    }
    showMoveModal = false;
    moveNodeId = null;
    moveTargetId = null;
  }
  
  function handleMoveCancel() {
    showMoveModal = false;
    moveNodeId = null;
    moveTargetId = null;
  }
  
  // Zoom & Pan handlers
  function handleWheel(e: WheelEvent) {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    const newScale = Math.max(0.3, Math.min(2, scale + delta));
    
    // Zoom toward cursor position
    if (svgContainer) {
      const rect = svgContainer.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      
      const scaleDiff = newScale - scale;
      translateX -= (x - translateX) * scaleDiff / scale;
      translateY -= (y - translateY) * scaleDiff / scale;
    }
    
    scale = newScale;
  }
  
  function handlePanStart(e: MouseEvent) {
    if (dragState.isDragging) return;
    if (e.target !== svgContainer && !(e.target as Element).classList.contains('mindmap-background')) return;
    
    panState = {
      isPanning: true,
      startX: e.clientX - translateX,
      startY: e.clientY - translateY
    };
  }
  
  function handlePanMove(e: MouseEvent) {
    if (!panState.isPanning) return;
    translateX = e.clientX - panState.startX;
    translateY = e.clientY - panState.startY;
  }
  
  function handlePanEnd() {
    panState = { isPanning: false, startX: 0, startY: 0 };
  }
  
  function handleResetZoom() {
    scale = 1;
    translateX = 0;
    translateY = 0;
  }
  
  function handleZoomIn() {
    scale = Math.min(2, scale + 0.2);
  }
  
  function handleZoomOut() {
    scale = Math.max(0.3, scale - 0.2);
  }
  
  // Global mouse move/up handlers
  function handleGlobalMouseMove(e: MouseEvent) {
    handleDragMove(e);
    handlePanMove(e);
  }
  
  function handleGlobalMouseUp() {
    handleDragEnd();
    handlePanEnd();
  }

  // ═══════════════════════════════════════════════════════════════════════════════
  // LIFECYCLE
  // ═══════════════════════════════════════════════════════════════════════════════
  
  onMount(() => {
    loadOrganizations();
    
    window.addEventListener('mousemove', handleGlobalMouseMove);
    window.addEventListener('mouseup', handleGlobalMouseUp);
    
    return () => {
      window.removeEventListener('mousemove', handleGlobalMouseMove);
      window.removeEventListener('mouseup', handleGlobalMouseUp);
    };
  });
</script>

<div class="flex flex-col gap-6 p-6 max-w-[2000px] mx-auto h-[calc(100vh-80px)]">
  <!-- Header -->
  <header class="flex justify-between items-center shrink-0">
    <div class="flex items-baseline gap-4">
      <h1 class="text-2xl font-bold text-foreground tracking-tight flex items-center gap-3">
        <span class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white text-lg shadow-lg shadow-indigo-500/25">
          🌐
        </span>
        조직 계층도
      </h1>
      <span class="text-sm text-muted-foreground font-medium bg-muted px-3 py-1 rounded-full">
        총 {stats.total.toLocaleString()}개 조직
      </span>
    </div>
    <div class="flex items-center gap-3">
      <!-- Zoom Controls -->
      <div class="flex items-center gap-1 bg-muted/50 rounded-lg p-1">
        <Button variant="ghost" size="sm" onclick={handleZoomOut} class="h-8 w-8 p-0">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35M8 11h6"/>
          </svg>
        </Button>
        <span class="text-xs font-medium text-muted-foreground w-12 text-center">
          {Math.round(scale * 100)}%
        </span>
        <Button variant="ghost" size="sm" onclick={handleZoomIn} class="h-8 w-8 p-0">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35M11 8v6M8 11h6"/>
          </svg>
        </Button>
        <div class="w-px h-4 bg-border mx-1"></div>
        <Button variant="ghost" size="sm" onclick={handleResetZoom} class="h-8 px-2 text-xs">
          리셋
        </Button>
      </div>
      
      <Button variant="outline" onclick={() => loadOrganizations()} disabled={loading}>
        <svg class="w-4 h-4 mr-2 {loading ? 'animate-spin' : ''}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 11-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/>
          <path d="M21 3v5h-5"/>
        </svg>
        새로고침
      </Button>
    </div>
  </header>

  <!-- Statistics Strip -->
  <div class="flex items-center gap-3 overflow-x-auto pb-2 shrink-0">
    {#each ORG_TYPE_ORDER as type}
      {@const colors = ORG_TYPE_COLORS[type]}
      <div class="flex items-center gap-2 px-4 py-2 rounded-xl {colors.bg} border {colors.border} whitespace-nowrap">
        <div class="w-6 h-6 rounded-lg bg-gradient-to-br {colors.gradient} flex items-center justify-center text-white text-xs font-bold shadow-sm">
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
    <div class="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/40 whitespace-nowrap">
      <div class="w-2 h-2 rounded-full bg-emerald-500"></div>
      <span class="text-sm font-medium text-emerald-600">정상</span>
      <span class="text-lg font-bold text-emerald-600">{stats.byStatus['ACTIVE'] || 0}</span>
    </div>
  </div>

  <!-- Main Content -->
  <div class="flex-1 grid grid-cols-1 lg:grid-cols-4 gap-6 min-h-0">
    <!-- Mindmap Canvas -->
    <div class="lg:col-span-3 relative overflow-hidden rounded-2xl border border-border bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800">
      {#if loading}
        <div class="absolute inset-0 flex flex-col items-center justify-center gap-4 text-muted-foreground bg-background/80 backdrop-blur-sm z-10">
          <div class="w-12 h-12 border-4 border-muted border-t-primary rounded-full animate-spin"></div>
          <span class="font-medium">조직 정보를 불러오는 중...</span>
        </div>
      {:else if error}
        <div class="absolute inset-0 flex flex-col items-center justify-center gap-4 text-destructive">
          <svg class="w-16 h-16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 8v4m0 4h.01"/>
          </svg>
          <span class="font-medium text-lg">{error}</span>
          <Button variant="outline" onclick={() => loadOrganizations()}>다시 시도</Button>
        </div>
      {:else}
        <svg
          bind:this={svgContainer}
          class="w-full h-full cursor-grab {panState.isPanning ? 'cursor-grabbing' : ''}"
          viewBox="0 0 {svgDimensions.width} {svgDimensions.height}"
          preserveAspectRatio="xMinYMin meet"
          onwheel={handleWheel}
          onmousedown={handlePanStart}
        >
          <!-- Background Pattern -->
          <defs>
            <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
              <circle cx="1" cy="1" r="0.5" fill="currentColor" class="text-muted-foreground/20"/>
            </pattern>
            <!-- Glow filter for nodes -->
            <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
              <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
              <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
            <!-- Shadow filter -->
            <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="4" stdDeviation="8" flood-opacity="0.15"/>
            </filter>
          </defs>
          
          <rect class="mindmap-background" width="100%" height="100%" fill="url(#grid)"/>
          
          <g transform="translate({translateX}, {translateY}) scale({scale})">
            <!-- Connections (Bezier Curves) -->
            {#each connections as conn}
              <path
                d={conn.path}
                fill="none"
                stroke="url(#gradient-{conn.from.orgType})"
                stroke-width="2"
                stroke-linecap="round"
                class="transition-all duration-300 {dropTargetId === conn.from.id ? 'stroke-[3] opacity-100' : 'opacity-60'}"
              />
            {/each}
            
            <!-- Gradient definitions for connections -->
            {#each ORG_TYPE_ORDER as type}
              <defs>
                <linearGradient id="gradient-{type}" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" class="{type === 'DISTRIBUTOR' ? 'stop-indigo-500' : type === 'AGENCY' ? 'stop-violet-500' : type === 'DEALER' ? 'stop-blue-500' : type === 'SELLER' ? 'stop-cyan-500' : 'stop-emerald-500'}" stop-color="currentColor"/>
                  <stop offset="100%" class="{type === 'DISTRIBUTOR' ? 'stop-violet-500' : type === 'AGENCY' ? 'stop-blue-500' : type === 'DEALER' ? 'stop-cyan-500' : type === 'SELLER' ? 'stop-emerald-500' : 'stop-teal-500'}" stop-color="currentColor"/>
                </linearGradient>
              </defs>
            {/each}
            
            <!-- Nodes -->
            {#each visibleNodes as node (node.id)}
              {@const isSelected = selectedNodeId === node.id}
              {@const isHovered = hoveredNodeId === node.id}
              {@const isEditing = editingNodeId === node.id}
              {@const isDragging = dragState.nodeId === node.id}
              {@const isDropTarget = dropTargetId === node.id}
              {@const hasChildren = node.children && node.children.length > 0}
              {@const offsetX = isDragging ? dragState.offsetX : 0}
              {@const offsetY = isDragging ? dragState.offsetY : 0}
              
              <g
                transform="translate({node.x + offsetX}, {node.y + offsetY})"
                class="transition-transform duration-150 {isDragging ? 'opacity-70' : ''}"
                onmouseenter={() => hoveredNodeId = node.id}
                onmouseleave={() => hoveredNodeId = null}
              >
                <!-- Node background with glass effect -->
                <rect
                  x="0"
                  y="0"
                  width={NODE_WIDTH}
                  height={NODE_HEIGHT}
                  rx="12"
                  class="fill-white dark:fill-slate-800 cursor-pointer transition-all duration-200
                    {isSelected ? 'stroke-2' : 'stroke-1'}
                    {isDropTarget ? 'stroke-emerald-500 stroke-[3] stroke-dashed' : ''}
                    {node.orgType === 'DISTRIBUTOR' ? 'stroke-indigo-400' : 
                     node.orgType === 'AGENCY' ? 'stroke-violet-400' : 
                     node.orgType === 'DEALER' ? 'stroke-blue-400' : 
                     node.orgType === 'SELLER' ? 'stroke-cyan-400' : 'stroke-emerald-400'}"
                  filter="{isSelected || isHovered ? 'url(#shadow)' : ''}"
                  onclick={() => handleNodeClick(node)}
                  ondblclick={() => handleNodeDoubleClick(node)}
                  onmousedown={(e) => handleDragStart(node.id, e)}
                />
                
                <!-- Type indicator bar -->
                <rect
                  x="0"
                  y="0"
                  width="6"
                  height={NODE_HEIGHT}
                  rx="3"
                  class="{node.orgType === 'DISTRIBUTOR' ? 'fill-indigo-500' : 
                         node.orgType === 'AGENCY' ? 'fill-violet-500' : 
                         node.orgType === 'DEALER' ? 'fill-blue-500' : 
                         node.orgType === 'SELLER' ? 'fill-cyan-500' : 'fill-emerald-500'}"
                />
                
                <!-- Status indicator -->
                <circle
                  cx={NODE_WIDTH - 12}
                  cy="12"
                  r="4"
                  class="{STATUS_COLORS[node.status]}"
                />
                
                {#if isEditing}
                  <!-- Edit mode -->
                  <foreignObject x="14" y="8" width={NODE_WIDTH - 28} height={NODE_HEIGHT - 16}>
                    <div class="flex flex-col gap-1">
                      <input
                        type="text"
                        bind:value={editName}
                        onkeydown={handleEditKeydown}
                        class="w-full px-2 py-1 text-sm font-semibold bg-white dark:bg-slate-700 border border-border rounded focus:outline-none focus:ring-2 focus:ring-primary"
                        autofocus
                      />
                      <div class="flex gap-1">
                        <select
                          bind:value={editStatus}
                          class="flex-1 px-1 py-0.5 text-xs bg-white dark:bg-slate-700 border border-border rounded"
                        >
                          <option value="ACTIVE">정상</option>
                          <option value="SUSPENDED">정지</option>
                          <option value="TERMINATED">해지</option>
                        </select>
                        <button onclick={handleEditSave} class="px-2 py-0.5 text-xs bg-primary text-primary-foreground rounded hover:bg-primary/90">✓</button>
                        <button onclick={handleEditCancel} class="px-2 py-0.5 text-xs bg-muted text-muted-foreground rounded hover:bg-muted/80">✕</button>
                      </div>
                    </div>
                  </foreignObject>
                {:else}
                  <!-- Display mode -->
                  <text
                    x="16"
                    y="24"
                    class="text-sm font-semibold fill-foreground pointer-events-none select-none"
                    dominant-baseline="middle"
                  >
                    {node.name.length > 12 ? node.name.slice(0, 12) + '...' : node.name}
                  </text>
                  
                  <text
                    x="16"
                    y="46"
                    class="text-xs fill-muted-foreground pointer-events-none select-none"
                    dominant-baseline="middle"
                  >
                    {ORG_TYPE_LABELS[node.orgType]} · {node.path.split('.').pop()}
                  </text>
                {/if}
                
                <!-- Expand/Collapse button -->
                {#if hasChildren}
                  <g
                    transform="translate({NODE_WIDTH - 8}, {NODE_HEIGHT / 2})"
                    onclick={(e) => handleToggleCollapse(node.id, e)}
                    class="cursor-pointer"
                  >
                    <circle r="10" class="fill-muted hover:fill-muted-foreground/20 transition-colors"/>
                    <text
                      class="text-xs font-bold fill-muted-foreground pointer-events-none"
                      text-anchor="middle"
                      dominant-baseline="middle"
                    >
                      {node.collapsed ? '+' : '−'}
                    </text>
                    {#if node.collapsed}
                      <text
                        x="0"
                        y="18"
                        class="text-[10px] fill-muted-foreground pointer-events-none"
                        text-anchor="middle"
                      >
                        {node.children.length}
                      </text>
                    {/if}
                  </g>
                {/if}
                
                <!-- Add child button (on hover) -->
                {#if (isHovered || isSelected) && !isEditing && ORG_TYPE_ORDER.indexOf(node.orgType) < ORG_TYPE_ORDER.length - 1}
                  <g
                    transform="translate({NODE_WIDTH + 20}, {NODE_HEIGHT / 2})"
                    onclick={() => handleAddChild(node.id)}
                    class="cursor-pointer opacity-0 hover:opacity-100 transition-opacity {isHovered ? 'opacity-70' : ''}"
                  >
                    <circle r="14" class="fill-primary/10 hover:fill-primary/20 stroke-primary stroke-1 transition-colors"/>
                    <text
                      class="text-sm font-bold fill-primary pointer-events-none"
                      text-anchor="middle"
                      dominant-baseline="middle"
                    >
                      +
                    </text>
                  </g>
                {/if}
              </g>
            {/each}
          </g>
        </svg>
      {/if}
      
      <!-- Legend -->
      <div class="absolute bottom-4 left-4 flex items-center gap-4 px-4 py-2 bg-white/90 dark:bg-slate-800/90 backdrop-blur-sm rounded-xl border border-border shadow-lg text-xs">
        <span class="text-muted-foreground font-medium">범례:</span>
        <div class="flex items-center gap-1">
          <div class="w-2 h-2 rounded-full bg-emerald-500"></div>
          <span>정상</span>
        </div>
        <div class="flex items-center gap-1">
          <div class="w-2 h-2 rounded-full bg-amber-500"></div>
          <span>정지</span>
        </div>
        <div class="flex items-center gap-1">
          <div class="w-2 h-2 rounded-full bg-rose-500"></div>
          <span>해지</span>
        </div>
        <div class="w-px h-4 bg-border"></div>
        <span class="text-muted-foreground">더블클릭: 편집 | 드래그: 이동 | 휠: 줌</span>
      </div>
    </div>

    <!-- Detail Panel -->
    <div class="lg:col-span-1">
      <Card class="h-full flex flex-col">
        <CardHeader class="pb-4 border-b border-border shrink-0">
          <CardTitle class="text-lg flex items-center gap-2">
            <svg class="w-5 h-5 text-muted-foreground" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5"/>
            </svg>
            조직 상세
          </CardTitle>
        </CardHeader>
        <CardContent class="flex-1 overflow-y-auto p-4">
          {#if selectedNode}
            {@const colors = ORG_TYPE_COLORS[selectedNode.orgType]}
            <div class="flex flex-col gap-6">
              <!-- Header -->
              <div class="flex items-start gap-3">
                <div class="w-12 h-12 rounded-xl bg-gradient-to-br {colors.gradient} flex items-center justify-center text-white font-bold text-lg shadow-lg">
                  {ORG_TYPE_LABELS[selectedNode.orgType].charAt(0)}
                </div>
                <div class="flex-1 min-w-0">
                  <h3 class="font-bold text-lg text-foreground truncate">{selectedNode.name}</h3>
                  <p class="text-sm text-muted-foreground font-mono">{selectedNode.orgCode}</p>
                </div>
              </div>
              
              <!-- Badges -->
              <div class="flex flex-wrap gap-2">
                <Badge class="{colors.bg} {colors.text} {colors.border} border">
                  {ORG_TYPE_LABELS[selectedNode.orgType]}
                </Badge>
                <Badge variant={selectedNode.status === 'ACTIVE' ? 'default' : selectedNode.status === 'SUSPENDED' ? 'secondary' : 'destructive'}>
                  {STATUS_LABELS[selectedNode.status]}
                </Badge>
              </div>
              
              <!-- Details Grid -->
              <div class="grid grid-cols-2 gap-4 p-4 bg-muted/30 rounded-xl">
                <div>
                  <span class="text-xs text-muted-foreground uppercase tracking-wide">계층 레벨</span>
                  <p class="font-semibold text-foreground mt-1">{selectedNode.level}단계</p>
                </div>
                <div>
                  <span class="text-xs text-muted-foreground uppercase tracking-wide">하위 조직</span>
                  <p class="font-semibold text-foreground mt-1">{selectedNode.children?.length || 0}개</p>
                </div>
                <div class="col-span-2">
                  <span class="text-xs text-muted-foreground uppercase tracking-wide">경로</span>
                  <p class="font-mono text-sm text-foreground mt-1 break-all">{selectedNode.path}</p>
                </div>
              </div>
              
              <!-- Children List -->
              {#if selectedNode.children && selectedNode.children.length > 0}
                <div>
                  <span class="text-sm font-medium text-muted-foreground mb-2 block">하위 조직</span>
                  <div class="flex flex-wrap gap-1.5 max-h-32 overflow-y-auto">
                    {#each selectedNode.children.slice(0, 15) as child}
                      {@const childColors = ORG_TYPE_COLORS[child.orgType]}
                      <button
                        onclick={() => selectedNodeId = child.id}
                        class="px-2 py-1 text-xs rounded-lg border transition-colors hover:bg-muted {childColors.bg} {childColors.border} {childColors.text}"
                      >
                        {child.name}
                      </button>
                    {/each}
                    {#if selectedNode.children.length > 15}
                      <span class="px-2 py-1 text-xs text-muted-foreground">
                        +{selectedNode.children.length - 15} more
                      </span>
                    {/if}
                  </div>
                </div>
              {/if}
              
              <!-- Actions -->
              <div class="flex flex-col gap-2 pt-4 border-t border-border">
                <Button class="w-full" onclick={() => selectedNode && handleOpenDetail(selectedNode)}>
                  <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                    <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                  </svg>
                  상세 정보 보기
                </Button>
                <Button variant="outline" class="w-full" onclick={() => selectedNode && handleNodeDoubleClick(selectedNode)}>
                  <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                  편집
                </Button>
                {#if ORG_TYPE_ORDER.indexOf(selectedNode.orgType) < ORG_TYPE_ORDER.length - 1}
                  <Button variant="secondary" class="w-full" onclick={() => selectedNode && handleAddChild(selectedNode.id)}>
                    <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 5v14m-7-7h14"/>
                    </svg>
                    하위 조직 추가
                  </Button>
                {/if}
              </div>
            </div>
          {:else}
            <div class="flex flex-col items-center justify-center h-full gap-4 text-muted-foreground">
              <div class="w-20 h-20 rounded-2xl bg-muted/50 flex items-center justify-center">
                <svg class="w-10 h-10 opacity-50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122"/>
                </svg>
              </div>
              <div class="text-center">
                <p class="font-medium">조직을 선택하세요</p>
                <p class="text-sm mt-1">마인드맵에서 노드를 클릭하면<br/>상세 정보가 표시됩니다</p>
              </div>
            </div>
          {/if}
        </CardContent>
      </Card>
    </div>
  </div>
</div>

<!-- Add Child Modal -->
{#if showAddModal}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
    <div class="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden">
      <div class="px-6 py-4 border-b border-border bg-muted/30">
        <h3 class="text-lg font-bold text-foreground">하위 조직 추가</h3>
        <p class="text-sm text-muted-foreground mt-1">새로운 하위 조직을 생성합니다</p>
      </div>
      <div class="p-6 flex flex-col gap-4">
        <div>
          <Label for="org-name" class="text-sm font-medium">조직명</Label>
          <Input
            id="org-name"
            value={newOrgName}
            oninput={(e) => newOrgName = e.currentTarget.value}
            placeholder="조직명을 입력하세요"
            class="mt-1.5"
          />
        </div>
        <div>
          <Label for="org-type" class="text-sm font-medium">조직 유형</Label>
          <select
            id="org-type"
            bind:value={newOrgType}
            class="mt-1.5 w-full h-10 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          >
            <option value="">유형 선택</option>
            {#each availableChildTypes as type}
              <option value={type}>{ORG_TYPE_LABELS[type]}</option>
            {/each}
          </select>
        </div>
      </div>
      <div class="px-6 py-4 border-t border-border bg-muted/30 flex justify-end gap-3">
        <Button variant="outline" onclick={() => showAddModal = false}>
          취소
        </Button>
        <Button onclick={handleAddChildSubmit} disabled={!newOrgName.trim() || !newOrgType}>
          생성
        </Button>
      </div>
    </div>
  </div>
{/if}

<!-- Move Confirmation Modal -->
{#if showMoveModal}
  {@const moveNode = mindmapNodes.find(n => n.id === moveNodeId)}
  {@const targetNode = mindmapNodes.find(n => n.id === moveTargetId)}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
    <div class="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden">
      <div class="px-6 py-4 border-b border-border bg-amber-500/10">
        <h3 class="text-lg font-bold text-foreground flex items-center gap-2">
          <svg class="w-5 h-5 text-amber-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
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
      <div class="px-6 py-4 border-t border-border bg-muted/30 flex justify-end gap-3">
        <Button variant="outline" onclick={handleMoveCancel}>
          취소
        </Button>
        <Button onclick={handleMoveConfirm}>
          이동
        </Button>
      </div>
    </div>
  </div>
{/if}

<!-- Toast Notifications -->
<div class="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
  {#each toasts as toast (toast.id)}
    <div
      class="px-4 py-3 rounded-xl shadow-lg backdrop-blur-sm flex items-center gap-3 animate-in slide-in-from-right duration-300
        {toast.type === 'success' ? 'bg-emerald-500/90 text-white' : 'bg-rose-500/90 text-white'}"
    >
      {#if toast.type === 'success'}
        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 6L9 17l-5-5"/>
        </svg>
      {:else}
        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 8v4m0 4h.01"/>
        </svg>
      {/if}
      <span class="font-medium">{toast.message}</span>
    </div>
  {/each}
</div>
