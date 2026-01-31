<script lang="ts">
  import { authStore } from '../lib/authStore';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  
  let username = $state('');
  let tenantId = $state('');
  
  $effect(() => {
    username = authStore.getUsername() || '';
    tenantId = authStore.getTenantId() || '';
  });
  
  function handleLogout() {
    if (confirm('Are you sure you want to logout?')) {
      authStore.logout();
      window.location.href = '/';
    }
  }
</script>

<header class="h-16 border-b bg-background px-6 flex items-center justify-between">
  <div class="flex items-center gap-4">
    <h2 class="text-lg font-semibold text-foreground">Settlement Management System</h2>
  </div>
  
  <div class="flex items-center gap-4">
    <div class="flex flex-col items-end gap-0.5">
      <span class="text-sm font-medium text-foreground">{username}</span>
      <Badge variant="secondary" class="text-xs">
        {tenantId}
      </Badge>
    </div>
    <Button variant="destructive" size="sm" onclick={handleLogout}>
      Logout
    </Button>
  </div>
</header>
