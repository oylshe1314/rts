import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import * as path from "path";

export default defineConfig({
    plugins: [vue()],
    base: "/rts/admin",
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    build: {
        outDir: 'dist',
        assetsDir: 'assets',
    },
    server: {
        proxy: {
            "/rts/admin/api": {
                target:"http://localhost:9000",
                changeOrigin:true,
                // rewrite:
            }
        }
    }
})
