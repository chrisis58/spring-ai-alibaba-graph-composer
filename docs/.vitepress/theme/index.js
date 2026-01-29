import DefaultTheme from 'vitepress/theme'
import CompatibilityMatrix from '../components/CompatibilityMatrix.vue'

export default {
    extends: DefaultTheme,
    enhanceApp({ app }) {
        app.component('CompatibilityMatrix', CompatibilityMatrix)
    }
}