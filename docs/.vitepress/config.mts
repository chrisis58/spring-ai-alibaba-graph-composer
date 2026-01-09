import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "SAA Graph Composer",
  description: "Declarative AI Agent Compose Extension for Spring AI Alibaba Graph",
  base: '/saa-graph-composer/',
  themeConfig: {
    nav: [
      { text: '指南', link: '/guide/getting-started' },
      { text: 'API 参考', link: '/reference/annotations' },
      { text: 'GitHub', link: 'https://github.com/chrisis58/saa-graph-composer' }
    ],

    sidebar: [
      {
        text: '基础指南',
        items: [
          { text: '简介', link: '/guide/introduction' },
          { text: '快速开始', link: '/guide/getting-started' },
          { text: '版本兼容性', link: '/guide/installation' }
        ]
      },
      {
        text: 'API 参考',
        items: [
          { text: '全局配置', link: '/reference/configuration' },
          { text: '图定义', link: '/reference/graph-definition' },
        ]
      },
      {
        text: '进阶用法',
        items: [
          { text: '手动与动态编译', link: '/advanced/dynamic-compilation' },
          { text: '生命周期钩子', link: '/advanced/hooks-lifecycle' },
          { text: '扩展 Graph Compiler', link: '/advanced/extend-compiler' }
        ]
      }
    ],

    lastUpdated: {
      text: '最后更新于',
      formatOptions: {
        dateStyle: 'short',
        timeStyle: 'medium'
      }
    },

    editLink: {
      pattern: 'https://github.com/chrisis58/saa-graph-composer/edit/main/docs/:path',
      text: '在 GitHub 上编辑此页'
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/chrisis58/saa-graph-composer' }
    ],

    footer: {
      message: 'Released under the Apache 2.0 License.',
      copyright: 'Copyright © 2026-present Teacy'
    }
  }
})
