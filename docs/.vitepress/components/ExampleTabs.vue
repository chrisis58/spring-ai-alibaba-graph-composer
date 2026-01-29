<template>
  <div class="example-tabs">
    <div class="tabs-header">

      <div class="tabs-nav">
        <button
            v-for="(tab, index) in tabs"
            :key="index"
            class="tab-btn"
            :class="{ active: index === activeIndex }"
            @click="activeIndex = index"
        >
          {{ tab.label }}
        </button>
      </div>

      <a v-if="tabs.length > 0" :href="currentGithubUrl" target="_blank" rel="noopener noreferrer" class="source-link">
        <span class="icon">
          <svg viewBox="0 0 16 16" width="14" height="14" fill="currentColor"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path></svg>
        </span>
        <span class="text">{{ currentDisplayTitle }}</span>
        <span class="arrow">↗</span>
      </a>
    </div>

    <div class="tabs-content">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, provide, reactive } from 'vue';

interface TabData {
  label: string;
  path: string;
  region?: string;
  title?: string;
}

const tabs = reactive<TabData[]>([]);
const activeIndex = ref(0);

const registerTab = (data: TabData) => {
  const index = tabs.length;
  tabs.push(data);
  return index;
};

provide('example-tabs-context', {
  registerTab,
  activeIndex
});

const GITHUB_BASE = 'https://github.com/chrisis58/spring-ai-alibaba-graph-composer/blob/main/examples/';

const currentGithubUrl = computed(() => {
  const item = tabs[activeIndex.value];
  if (!item) return '';
  const cleanPath = item.path.replace(/^\//, '');
  return `${GITHUB_BASE}${cleanPath}`;
});

const currentDisplayTitle = computed(() => {
  const item = tabs[activeIndex.value];
  if (!item) return '';
  if (item.title) return item.title;
  const fileName = item.path.split('/').pop() || '';
  return item.region ? `${fileName} (${item.region})` : fileName;
});
</script>

<style scoped>
.example-tabs {
  margin: 16px 0;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  background-color: var(--vp-c-bg-soft);
  overflow: hidden;
}

.tabs-header {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  border-bottom: 1px solid var(--vp-c-divider);
  background-color: var(--vp-c-bg-alt);
  min-height: 40px;
}

.tabs-nav {
  display: flex;
}

.tab-btn {
  margin-left: 10px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--vp-c-text-2);
  background: transparent;
  border: none;
  border-right: 1px solid transparent;
  cursor: pointer;
  transition: all 0.25s;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.tab-btn:hover {
  color: var(--vp-c-text-1);
}

.tab-btn.active {
  color: var(--vp-c-brand-1);
  background-color: var(--vp-c-bg-soft);
  border-bottom-color: var(--vp-c-brand-1);
}

.source-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  font-size: 12px;
  color: var(--vp-c-text-2);
  text-decoration: none;
  border-left: 1px solid var(--vp-c-divider);
  transition: color 0.2s;
}
.source-link:hover {
  color: var(--vp-c-brand-1);
}
.icon svg { display: block; }
.arrow { font-family: sans-serif; font-size: 11px; }

.tabs-content :deep(.example-wrapper) {
  margin: 0 !important;
  border: none !important;
  border-radius: 0 !important;
}
</style>