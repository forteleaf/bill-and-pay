export interface Tab {
  id: string;
  title: string;
  icon: string;
  component: string;
  closeable: boolean;
  props?: Record<string, any>;
}

interface TabStoreState {
  tabs: Tab[];
  activeTabId: string;
}

class TabStore {
  private state: TabStoreState = {
    tabs: [],
    activeTabId: '',
  };

  private readonly MAX_TABS = 10;
  private readonly STORAGE_KEY = 'billpay-tabs';

  constructor() {
    this.loadFromStorage();
  }

  private loadFromStorage() {
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        try {
          const parsed = JSON.parse(stored);
          this.state = {
            tabs: parsed.tabs || [],
            activeTabId: parsed.activeTabId || '',
          };
        } catch (e) {
          console.error('Failed to parse tabs from storage:', e);
          this.state = { tabs: [], activeTabId: '' };
        }
      }
    }
  }

  private saveToStorage() {
    if (typeof window !== 'undefined') {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.state));
    }
  }

  getTabs(): Tab[] {
    return this.state.tabs;
  }

  getActiveTabId(): string {
    return this.state.activeTabId;
  }

  getActiveTab(): Tab | undefined {
    return this.state.tabs.find((tab) => tab.id === this.state.activeTabId);
  }

  openTab(tab: Tab): void {
    // Check if tab with same ID already exists
    const existingTab = this.state.tabs.find((t) => t.id === tab.id);
    if (existingTab) {
      this.focusTab(tab.id);
      return;
    }

    // Check if at max tabs
    if (this.state.tabs.length >= this.MAX_TABS) {
      alert('최대 10개의 탭만 열 수 있습니다.');
      return;
    }

    // Add new tab and focus it
    this.state.tabs.push(tab);
    this.state.activeTabId = tab.id;
    this.saveToStorage();
  }

  closeTab(id: string): void {
    const tab = this.state.tabs.find((t) => t.id === id);
    if (!tab || !tab.closeable) {
      return;
    }

    this.state.tabs = this.state.tabs.filter((t) => t.id !== id);

    // If closed tab was active, focus the last remaining tab
    if (this.state.activeTabId === id) {
      this.state.activeTabId = this.state.tabs.length > 0 ? this.state.tabs[this.state.tabs.length - 1].id : '';
    }

    this.saveToStorage();
  }

  focusTab(id: string): void {
    const tab = this.state.tabs.find((t) => t.id === id);
    if (tab) {
      this.state.activeTabId = id;
      this.saveToStorage();
    }
  }
}

export const tabStore = new TabStore();
