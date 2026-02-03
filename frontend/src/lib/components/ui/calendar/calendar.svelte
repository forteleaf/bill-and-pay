<script lang="ts">
  import { Calendar as CalendarPrimitive } from "bits-ui";
  import { CalendarDate, getLocalTimeZone, type DateValue } from "@internationalized/date";
  import { cn } from "$lib/utils";

  interface Props {
    value?: Date | undefined;
    onValueChange?: (date: Date | undefined) => void;
    placeholder?: Date;
    class?: string;
  }

  let { 
    value = $bindable(), 
    onValueChange, 
    placeholder,
    class: className 
  }: Props = $props();

  const weekdays = ["일", "월", "화", "수", "목", "금", "토"];

  // Convert JS Date to CalendarDate
  function toCalendarDate(date: Date | undefined): CalendarDate | undefined {
    if (!date) return undefined;
    return new CalendarDate(date.getFullYear(), date.getMonth() + 1, date.getDate());
  }

  // Convert CalendarDate to JS Date
  function toJSDate(calDate: DateValue | undefined): Date | undefined {
    if (!calDate) return undefined;
    return calDate.toDate(getLocalTimeZone());
  }

  // Internal calendar value
  let calendarValue = $derived(toCalendarDate(value));

  // Internal placeholder
  let calendarPlaceholder = $derived(toCalendarDate(placeholder || new Date()));

  function handleValueChange(val: DateValue | DateValue[] | undefined) {
    // Handle single date selection (ignore arrays)
    if (Array.isArray(val)) return;
    const jsDate = toJSDate(val);
    if (onValueChange) {
      onValueChange(jsDate);
    }
  }
</script>

<CalendarPrimitive.Root
  type="single"
  value={calendarValue}
  onValueChange={handleValueChange}
  placeholder={calendarPlaceholder}
  weekdayFormat="short"
  locale="ko-KR"
  class={cn("p-3", className)}
>
  {#snippet children({ months })}
    <CalendarPrimitive.Header class="relative flex w-full items-center justify-between pt-1">
      <CalendarPrimitive.PrevButton
        class="inline-flex h-7 w-7 items-center justify-center rounded-md border border-input bg-transparent p-0 opacity-50 hover:opacity-100 hover:bg-accent disabled:pointer-events-none"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6"/>
        </svg>
      </CalendarPrimitive.PrevButton>
      <CalendarPrimitive.Heading class="text-sm font-medium">
        {#snippet children({ headingValue })}
          {headingValue || '-'}
        {/snippet}
      </CalendarPrimitive.Heading>
      <CalendarPrimitive.NextButton
        class="inline-flex h-7 w-7 items-center justify-center rounded-md border border-input bg-transparent p-0 opacity-50 hover:opacity-100 hover:bg-accent disabled:pointer-events-none"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m9 18 6-6-6-6"/>
        </svg>
      </CalendarPrimitive.NextButton>
    </CalendarPrimitive.Header>

    {#each months as month}
      <CalendarPrimitive.Grid class="mt-4 w-full border-collapse space-y-1">
        <CalendarPrimitive.GridHead>
          <CalendarPrimitive.GridRow class="flex">
            {#each weekdays as weekday}
              <CalendarPrimitive.HeadCell
                class="w-9 rounded-md text-[0.8rem] font-normal text-muted-foreground"
              >
                {weekday}
              </CalendarPrimitive.HeadCell>
            {/each}
          </CalendarPrimitive.GridRow>
        </CalendarPrimitive.GridHead>
        <CalendarPrimitive.GridBody>
          {#each month.weeks as week}
            <CalendarPrimitive.GridRow class="mt-2 flex w-full">
              {#each week as day}
                <CalendarPrimitive.Cell
                  date={day}
                  month={month.value}
                  class="relative h-9 w-9 p-0 text-center text-sm focus-within:relative focus-within:z-20 [&:has([data-selected])]:bg-accent first:[&:has([data-selected])]:rounded-l-md last:[&:has([data-selected])]:rounded-r-md [&:has([data-selected][data-outside-month])]:bg-accent/50"
                >
                  <CalendarPrimitive.Day
                    class={cn(
                      "inline-flex h-9 w-9 items-center justify-center rounded-md p-0 text-sm font-normal ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
                      "data-[disabled]:pointer-events-none data-[disabled]:opacity-50",
                      "data-[selected]:bg-primary data-[selected]:text-primary-foreground data-[selected]:hover:bg-primary data-[selected]:hover:text-primary-foreground data-[selected]:focus:bg-primary data-[selected]:focus:text-primary-foreground",
                      "data-[outside-month]:text-muted-foreground data-[outside-month]:opacity-50",
                      "data-[today]:bg-accent data-[today]:text-accent-foreground"
                    )}
                  />
                </CalendarPrimitive.Cell>
              {/each}
            </CalendarPrimitive.GridRow>
          {/each}
        </CalendarPrimitive.GridBody>
      </CalendarPrimitive.Grid>
    {/each}
  {/snippet}
</CalendarPrimitive.Root>
