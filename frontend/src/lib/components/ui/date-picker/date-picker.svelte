<script lang="ts">
  import { Popover as PopoverPrimitive } from "bits-ui";
  import { Calendar } from "$lib/components/ui/calendar";
  import { cn } from "$lib/utils";
  import { format, parse, isValid } from "date-fns";

  interface Props {
    value?: string;
    onchange?: (date: string) => void;
    placeholder?: string;
    disabled?: boolean;
    class?: string;
  }

  let { 
    value = '', 
    onchange, 
    placeholder = '날짜 선택', 
    disabled = false, 
    class: className = '' 
  }: Props = $props();

  let open = $state(false);

  // Parse value string to Date object for calendar
  function parseValue(val: string): Date | undefined {
    if (!val) return undefined;
    try {
      // Try YYYY/MM/DD format first
      let parsed = parse(val, 'yyyy/MM/dd', new Date());
      if (isValid(parsed)) return parsed;
      // Also try YYYY-MM-DD format for compatibility
      parsed = parse(val, 'yyyy-MM-dd', new Date());
      if (isValid(parsed)) return parsed;
      return undefined;
    } catch {
      return undefined;
    }
  }

  let selectedDate = $derived(parseValue(value));

  function handleSelect(date: Date | undefined) {
    if (date) {
      const formatted = format(date, 'yyyy/MM/dd');
      onchange?.(formatted);
    }
    open = false;
  }

  // Display format
  function displayValue(val: string): string {
    if (!val) return '';
    const date = parseValue(val);
    if (date) {
      return format(date, 'yyyy/MM/dd');
    }
    return val;
  }
</script>

<PopoverPrimitive.Root bind:open>
  <PopoverPrimitive.Trigger {disabled}>
    <button
      type="button"
      class={cn(
        "inline-flex h-10 w-[180px] items-center justify-start gap-2 whitespace-nowrap rounded-md border border-input bg-background px-3 py-2 text-sm font-normal ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
        !value && "text-muted-foreground",
        className
      )}
      {disabled}
    >
      <svg 
        class="h-4 w-4 shrink-0" 
        xmlns="http://www.w3.org/2000/svg" 
        viewBox="0 0 24 24" 
        fill="none" 
        stroke="currentColor" 
        stroke-width="2" 
        stroke-linecap="round" 
        stroke-linejoin="round"
      >
        <rect width="18" height="18" x="3" y="4" rx="2" ry="2"/>
        <line x1="16" x2="16" y1="2" y2="6"/>
        <line x1="8" x2="8" y1="2" y2="6"/>
        <line x1="3" x2="21" y1="10" y2="10"/>
      </svg>
      <span class="truncate">{value ? displayValue(value) : placeholder}</span>
    </button>
  </PopoverPrimitive.Trigger>
  <PopoverPrimitive.Content 
    class="z-50 w-auto rounded-md border border-border bg-popover p-0 text-popover-foreground shadow-md outline-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2"
    align="start"
  >
    <Calendar 
      value={selectedDate}
      onValueChange={handleSelect}
      placeholder={new Date()}
    />
  </PopoverPrimitive.Content>
</PopoverPrimitive.Root>
