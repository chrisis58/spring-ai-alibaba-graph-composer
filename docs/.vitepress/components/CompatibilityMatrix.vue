<template>
  <div class="compatibility-container">
    <div v-if="loading" class="custom-block info">
      <p>正在同步最新环境兼容性数据...</p>
    </div>

    <div v-else-if="error" class="custom-block danger">
      <p>无法加载兼容性矩阵，请稍后重试或前往 GitHub Action 查看。</p>
    </div>

    <div v-else class="matrix-wrapper">
      <table>
        <thead>
        <tr>
          <th>Spring Boot</th>
          <th>Spring AI Alibaba</th>
          <th>测试状态</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="item in matrix" :key="item.boot + item.saa">
          <td><code>{{ item.boot }}</code></td>
          <td><code>{{ item.saa }}</code></td>
          <td>
            <span v-if="item.status === 'success'" class="status-pass">✅</span>
            <span v-else class="status-fail">❌</span>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const matrix = ref([])
const loading = ref(true)
const error = ref(false)

const DATA_URL = 'https://cdn.jsdelivr.net/gh/chrisis58/spring-ai-alibaba-graph-composer@main/metadata/compatibility.json'

onMounted(async () => {
  try {
    const cacheInterval = 1000 * 60 * 10;
    const t = Math.floor(Date.now() / cacheInterval);

    const response = await fetch(`${DATA_URL}?t=${t}`)
    if (response.ok) {
      matrix.value = await response.json()
    } else {
      error.value = true
    }
  } catch (err) {
    console.error('Fetch compatibility data failed:', err)
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.matrix-wrapper {
  margin: 1.5rem 0;
  overflow-x: auto;
}

.status-pass {
  color: var(--vp-c-brand-1);
  font-weight: 600;
}

.status-fail {
  color: var(--vp-c-danger-1);
  font-weight: 600;
}

code {
  padding: 0.2rem 0.4rem;
  background-color: var(--vp-c-bg-soft);
  border-radius: 4px;
  font-size: 0.9em;
}

table {
  width: 100%;
  display: table;
  margin: 0;
}

th {
  text-align: center !important;
}

td {
  text-align: center !important;
  vertical-align: middle;
}
</style>