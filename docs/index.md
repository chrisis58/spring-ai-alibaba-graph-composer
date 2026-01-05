---
layout: home

hero:
  name: "SAA Graph Composer"
  text: "Spring AI Alibaba Graph 的声明式编排扩展"
  tagline: 注解驱动 · Spring 深度集成 · 混合编排架构
  actions:
    - theme: brand
      text: "🚀 快速开始"
      link: /guide/getting-started
    - theme: alt
      text: GitHub 源码
      link: https://github.com/chrisis58/saa-graph-composer

features:
  - title: 🧩 结构即代码 (Code as Graph)
    details: 将拓扑结构映射为代码结构。通过 @GraphNode 直观描述流转路径，实现“所见即所得”的开发体验，像阅读流程图一样阅读代码，告别晦涩的构建脚本。
  - title: 🍃 Spring 深度集成
    details: 遵循 Spring 标准开发模式。图定义类与编译后的图实例均被托管为标准 Bean，支持原生依赖注入、AOP 与配置管理，无缝融入现有架构。
  - title: 🔌 混合编排能力
    details: 兼顾便捷与灵活。既支持纯注解模式快速定义静态流程，也能通过生命周期钩子（Lifecycle Hooks）介入底层 API，实现复杂的动态连线与逻辑补全。
---

<style>
:root {
    --vp-home-hero-name-color: transparent;
    --vp-home-hero-name-background: -webkit-linear-gradient(120deg, #7b5aff 30%, #52a9ff);
}
</style>