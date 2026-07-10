/**
 * index.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import type {RouteLocationNormalized, Router, RouteRecordRaw} from "vue-router";
import {createRouter, createWebHistory} from "vue-router";

declare module 'vue-router' {
    interface RouteMeta {
        title: string;
        tabView: boolean;
        closable: boolean
    }
}

const routes: RouteRecordRaw[] = [
    {
        path: "/",
        redirect: '/index'
    },
    {
        name: 'LoginPage',
        path: '/login',
        component: () => import('@/pages/LoginPage.vue'),
        meta: {
            title: '登录',
            tabView: true,
            closable: true,
        }
    },
    {
        name: 'IndexPage',
        path: "/index",
        component: () => import('@/pages/IndexPage.vue'),
        children: [
            {
                name: 'HomeView',
                path: '/home',
                component: () => import('@/pages/views/HomeView.vue'),
                meta: {
                    title: "首页",
                    tabView: true,
                    closable: false,
                }
            },
            {
                name: 'NotFound',
                path: '/404',
                component: () => import('@/pages/views/NotFound.vue'),
                meta: {
                    title: "404",
                    tabView: true,
                    closable: true,
                }
            },
            {
                path: '/menu/index',
                name: 'MenuIndex',
                component: () => import('@/pages/views/menu/MenuIndex.vue'),
                meta: {
                    tabView: true,
                    title: "菜单列表",
                    closable: true,
                }
            },
            {
                path: '/admin/role/index',
                name: 'RoleIndex',
                component: () => import('@/pages/views/admin/role/RoleIndex.vue'),
                meta: {
                    tabView: true,
                    title: "角色列表",
                    closable: true,
                }
            },
            {
                path: '/admin/index',
                name: 'AdminIndex',
                component: () => import('@/pages/views/admin/AdminIndex.vue'),
                meta: {
                    tabView: true,
                    title: "管理员列表",
                    closable: true,
                }
            },
            {
                path: '/operation/record/index',
                name: 'OperationRecordIndex',
                component: () => import('@/pages/views/operation/record/RecordIndex.vue'),
                meta: {
                    tabView: true,
                    title: "操作记录列表",
                    closable: true,
                }
            },
            {
                name: 'DetailsView',
                path: '/setting/details',
                component: () => import('@/pages/views/setting/DetailsView.vue'),
                meta: {
                    title: "用户详情",
                    tabView: true,
                    closable: true,
                }
            },
            {
                name: 'ChangePassword',
                path: '/setting/password',
                component: () => import('@/pages/views/setting/ChangePassword.vue'),
                meta: {
                    title: "修改密码",
                    tabView: true,
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

router.beforeEach(async (to: RouteLocationNormalized) => {
    const {useUserStore} = await import("@/store/user.ts");
    const userStore = useUserStore();

    if (to.meta && to.meta.title) {
        document.title = to.meta.title;
    }

    if (to.name === 'LoginPage') {
        return;
    } else {
        if (!userStore.getToken()) {
            return {name: 'LoginPage'};
        }
    }

    const {useTabsStore} = await import("@/store/tabs.ts");
    const tabsStore = useTabsStore();

    if (to.meta.tabView && to.name !== 'HomeView') {
        if (to.name && tabsStore.getIndex(String(to.name)) < 0) {
            tabsStore.addCard({
                id: tabsStore.getSize() + 1,
                name: String(to.name),
                path: String(to.path),
                title: to.meta.title,
                closable: to.meta.closable,
            });
        }
    }
});

export default router;