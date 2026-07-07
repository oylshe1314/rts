/**
 * avatars.ts
 * create by Snake as 2026-07-07
 * @description:
 */

const avatarModules: Record<string, string> = import.meta.glob('@/assets/images/avatars/*.png', {eager: true, import: 'default'});

const avatars: Record<string, string> = {};

Object.entries(avatarModules).forEach(([path, url]): void => {
    const li = path.lastIndexOf("/");
    const filename =  (li < 0 ? path : path.substring(li + 1,)).replace('.png', '');
    avatars[filename] = url;
});

export function getAvatar(name: string): string {
    if (!name || name === '') {
        return avatars['avatar1'];
    }

    const url = avatars[name];
    return url ? url : avatars['avatar1'];
}

export default avatars;