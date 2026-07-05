/**
 * index.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import {createRouter, createWebHistory} from "vue-router";
import type {Router, RouteRecordRaw, RouteLocationNormalized} from "vue-router"

import user from "@/store/user";

declare module 'vue-router' {
    interface RouteMeta {
        title?: string;
        tabView?: boolean;
        closable?: boolean
    }
}

const routes: RouteRecordRaw[] = [
    {
        path: "/",
        redirect: '/index'
    },
    {
        path: '/login',
        name: 'LoginPage',
        component: () => import('@/pages/LoginPage.vue'),
        meta: {
            title: '登录',
        }
    },
    {
        path: "/index",
        name: 'IndexPage',
        component: () => import('@/pages/IndexPage.vue'),
        children: [
            {
                path: '/home',
                name: 'HomeView',
                component: () => import('@/pages/views/HomeView.vue'),
                meta: {
                    tabView: true,
                    title: "首页"
                }
            },
            {
                path: '/404',
                name: 'NotFound',
                component: () => import('@/pages/views/NotFound.vue'),
                meta: {
                    tabView: true,
                    title: "404",
                    closable: true,
                }
            },
        ]
    }
];

const router: Router = createRouter({
    routes: routes,
    history: createWebHistory(import.meta.env.BASE_URL),
});

router.beforeEach((to: RouteLocationNormalized)  => {
    if (to.meta && to.meta.title) {
        document.title = to.meta.title;
    }
    if (to.name === 'LoginPage' || user.getToken) {
        return;
    } else {
        return {name: 'LoginPage'};
    }
});

export default router;