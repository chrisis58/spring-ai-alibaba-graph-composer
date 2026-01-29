import DefaultTheme from 'vitepress/theme'
import CompatibilityMatrix from '../components/CompatibilityMatrix.vue'
import ExampleWrapper from "../components/ExampleWrapper.vue";
import ExampleTabs from "../components/ExampleTabs.vue";

export default {
    extends: DefaultTheme,
    enhanceApp({ app }) {
        app.component('CompatibilityMatrix', CompatibilityMatrix)
        app.component('ExampleWrapper', ExampleWrapper)
        app.component('ExampleTabs', ExampleTabs)
    }
}