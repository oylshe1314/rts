/**
 * index.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import {createApp} from 'vue'
import App from './App.vue'

import elementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as elementPlusIcons from '@element-plus/icons-vue'

import router from './router'
import store from "./store";

const app = createApp(App);

app.use(elementPlus);

app.config.globalProperties.$icons = []
for (const [name, component] of Object.entries(elementPlusIcons)) {
    app.component(name, component);
    app.config.globalProperties.$icons.push(component);
}

app.use(router);
app.use(store);

createApp(App).mount('#app');
