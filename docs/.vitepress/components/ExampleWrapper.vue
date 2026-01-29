<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue';

interface Props {
  path: string;
  region?: string;
  title?: string;
  label?: string;
}

const props = defineProps<Props>();

const tabsContext = inject<any>('example-tabs-context', null);

const myIndex = ref(-1);

if (tabsContext) {
  const tabLabel = props.label || props.path.split('/').pop() || 'Tab';

  myIndex.value = tabsContext.registerTab({
    label: tabLabel,
    path: props.path,
    region: props.region,
    title: props.title
  });
}

const isTabItem = computed(() => tabsContext !== null);

const isActive = computed(() => {
  if (!tabsContext) return true;
  return tabsContext.activeIndex.value === myIndex.value;
});

const GITHUB_BASE = 'https://github.com/chrisis58/spring-ai-alibaba-graph-composer/blob/main/examples/';

const githubUrl = computed(() => {
  const cleanPath = props.path.replace(/^\//, '');
  return `${GITHUB_BASE}${cleanPath}`;
});

const displayTitle = computed(() => {
  if (props.title) return props.title;
  const fileName = props.path.split('/').pop() || '';
  return props.region ? `${fileName} (${props.region})` : fileName;
});
</script>

<template>
  <div class="example-wrapper" v-show="isActive">

    <div class="example-header" v-if="!isTabItem">
      <a :href="githubUrl" target="_blank" rel="noopener noreferrer" class="source-link">
        <span class="icon">
          <svg viewBox="0 0 16 16" width="16" height="16" fill="currentColor"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path></svg>
        </span>
        <span class="text">{{ displayTitle }}</span>
        <span class="arrow">↗</span>
      </a>
    </div>

    <div class="example-content">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.example-wrapper {
  margin: 16px 0;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  background-color: var(--vp-c-bg-soft);
  overflow: hidden;
}

.example-header {
  padding: 8px 12px;
  border-bottom: 1px solid var(--vp-c-divider);
  background-color: var(--vp-c-bg-alt);
  font-size: 13px;
  display: flex;
  justify-content: flex-end;
}

.source-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--vp-c-text-2);
  text-decoration: none;
  transition: color 0.2s;
}
.source-link:hover {
  color: var(--vp-c-brand-1);
}
.icon svg { display: block; }
.arrow { font-family: sans-serif; font-size: 12px; }

.example-content :deep(div[class*='language-']) {
  margin: 0 !important;
  border-radius: 0 !important;
  border: none !important;
}
</style>