<script lang="ts">
  import { Popover as PopoverPrimitive, RangeCalendar } from "bits-ui";
  import { CalendarDate, getLocalTimeZone, type DateValue } from "@internationalized/date";
  import { cn } from "$lib/utils";
  import { format, parse, isValid } from "date-fns";

  type DateRange = {
    start: DateValue | undefined;
    end: DateValue | undefined;
  };

  interface Props {
    startDate?: string;
    endDate?: string;
    onchange?: (start: string, end: string) => void;
    placeholder?: string;
    disabled?: boolean;
    class?: string;
  }

  let { 
    startDate = '', 
    endDate = '',
    onchange, 
    placeholder = '기간 선택', 
    disabled = false, 
    class: className = '' 
  }: Props = $props();

  let open = $state(false);

  const weekdays = ["일", "월", "화", "수", "목", "금", "토"];

  // Parse YYYY/MM/DD or YYYY-MM-DD string to CalendarDate
  function parseToCalendarDate(val: string): CalendarDate | undefined {
    if (!val) return undefined;
    try {
      // Try YYYY/MM/DD format first
      let parsed = parse(val, 'yyyy/MM/dd', new Date());
      if (isValid(parsed)) {
        return new CalendarDate(parsed.getFullYear(), parsed.getMonth() + 1, parsed.getDate());
      }
      // Also try YYYY-MM-DD format for compatibility
      parsed = parse(val, 'yyyy-MM-dd', new Date());
      if (isValid(parsed)) {
        return new CalendarDate(parsed.getFullYear(), parsed.getMonth() + 1, parsed.getDate());
      }
      return undefined;
    } catch {
      return undefined;
    }
  }

  // Convert CalendarDate to YYYY/MM/DD string
  function toDateString(calDate: DateValue | undefined): string {
    if (!calDate) return '';
    const jsDate = calDate.toDate(getLocalTimeZone());
    return format(jsDate, 'yyyy/MM/dd');
  }

  // Derive the date range for the calendar
  let calendarValue = $derived<DateRange>({
    start: parseToCalendarDate(startDate),
    end: parseToCalendarDate(endDate)
  });

  function handleValueChange(range: DateRange | undefined) {
    if (!range) {
      onchange?.('', '');
      return;
    }
    
    const start = toDateString(range.start);
    const end = toDateString(range.end);
    onchange?.(start, end);
    
    // Close popover when both dates are selected
    if (range.start && range.end) {
      open = false;
    }
  }

  // Display format
  function displayValue(): string {
    if (!startDate && !endDate) return '';
    const startDisplay = startDate || '';
    const endDisplay = endDate || '';
    if (startDisplay && endDisplay) {
      return `${startDisplay} ~ ${endDisplay}`;
    }
    if (startDisplay) {
      return `${startDisplay} ~`;
    }
    return '';
  }
</script>

<PopoverPrimitive.Root bind:open>
  <PopoverPrimitive.Trigger {disabled}>
    <button
      type="button"
      class={cn(
        "inline-flex h-10 w-[280px] items-center justify-start gap-2 whitespace-nowrap rounded-md border border-input bg-background px-3 py-2 text-sm font-normal ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
        !startDate && !endDate && "text-muted-foreground",
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
      <span class="truncate">{displayValue() || placeholder}</span>
    </button>
  </PopoverPrimitive.Trigger>
  <PopoverPrimitive.Content 
    class="z-[40] w-auto rounded-md border border-border bg-popover p-0 text-popover-foreground shadow-md outline-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2"
    align="start"
  >
    <RangeCalendar.Root
      value={calendarValue}
      onValueChange={handleValueChange}
      weekdayFormat="short"
      locale="ko-KR"
      class="p-3"
    >
      {#snippet children({ months })}
        <RangeCalendar.Header class="relative flex w-full items-center justify-between pt-1">
          <RangeCalendar.PrevButton
            class="inline-flex h-7 w-7 items-center justify-center rounded-md border border-input bg-transparent p-0 opacity-50 hover:opacity-100 hover:bg-accent disabled:pointer-events-none"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m15 18-6-6 6-6"/>
            </svg>
          </RangeCalendar.PrevButton>
          <RangeCalendar.Heading class="text-sm font-medium">
            {#snippet children({ headingValue })}
              {headingValue || '-'}
            {/snippet}
          </RangeCalendar.Heading>
          <RangeCalendar.NextButton
            class="inline-flex h-7 w-7 items-center justify-center rounded-md border border-input bg-transparent p-0 opacity-50 hover:opacity-100 hover:bg-accent disabled:pointer-events-none"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m9 18 6-6-6-6"/>
            </svg>
          </RangeCalendar.NextButton>
        </RangeCalendar.Header>

        {#each months as month}
          <RangeCalendar.Grid class="mt-4 w-full border-collapse space-y-1">
            <RangeCalendar.GridHead>
              <RangeCalendar.GridRow class="flex">
                {#each weekdays as weekday}
                  <RangeCalendar.HeadCell
                    class="w-9 rounded-md text-[0.8rem] font-normal text-muted-foreground"
                  >
                    {weekday}
                  </RangeCalendar.HeadCell>
                {/each}
              </RangeCalendar.GridRow>
            </RangeCalendar.GridHead>
            <RangeCalendar.GridBody>
              {#each month.weeks as week}
                <RangeCalendar.GridRow class="mt-2 flex w-full">
                  {#each week as day}
                    <RangeCalendar.Cell
                      date={day}
                      month={month.value}
                      class="relative h-9 w-9 p-0 text-center text-sm focus-within:relative focus-within:z-20 [&:has([data-selected])]:bg-accent [&:has([data-selection-start])]:rounded-l-md [&:has([data-selection-end])]:rounded-r-md [&:has([data-selected][data-outside-month])]:bg-accent/50"
                    >
                      <RangeCalendar.Day
                        class={cn(
                          "inline-flex h-9 w-9 items-center justify-center rounded-md p-0 text-sm font-normal ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
                          "data-[disabled]:pointer-events-none data-[disabled]:opacity-50",
                          "data-[selected]:bg-primary data-[selected]:text-primary-foreground",
                          "data-[selection-start]:bg-primary data-[selection-start]:text-primary-foreground data-[selection-start]:rounded-l-md",
                          "data-[selection-end]:bg-primary data-[selection-end]:text-primary-foreground data-[selection-end]:rounded-r-md",
                          "data-[highlighted]:bg-accent data-[highlighted]:text-accent-foreground data-[highlighted]:rounded-none",
                          "data-[outside-month]:text-muted-foreground data-[outside-month]:opacity-50",
                          "data-[today]:bg-accent data-[today]:text-accent-foreground"
                        )}
                      />
                    </RangeCalendar.Cell>
                  {/each}
                </RangeCalendar.GridRow>
              {/each}
            </RangeCalendar.GridBody>
          </RangeCalendar.Grid>
        {/each}
      {/snippet}
    </RangeCalendar.Root>
  </PopoverPrimitive.Content>
</PopoverPrimitive.Root>
