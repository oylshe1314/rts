/**
 * store.ts
 * create by Snake as 2026-07-04
 * @description:
 */

import {defineStore} from "pinia";

class TabCard {
    id: number = 0;
    name: string = "";
    title: string = "";
    path: string = "";
    closable: boolean = false;
}

const homeTabCard: TabCard = {
    id: 0,
    name: 'HomeView',
    title: '首页',
    path: '/home',
    closable: false,
}

class TabCards {
    names: string[] = [homeTabCard.name];
    cards: TabCard[] = [homeTabCard];
}

const tabsStore = defineStore("tabs", {
    state: () => {
        return new TabCards();
    },
    actions: {
        addCard(card: TabCard): void {
            this.names.push(card.name);
            this.cards.push(card);
        },
        removeCard(index: number): boolean {
            this.names.splice(index, 1);
            this.cards.splice(index, 1);
            return true;
        },
        clear(): void {
            this.$reset();
        }
    },
    getters: {
        getNames(): string[] {
            return this.names;
        },
        getCards(): TabCard[] {
            return this.cards;
        },
        getIndex() {
            return (name: string): number => {
                for (const i in this.names) {
                    if (this.names[i] === name) {
                        return Number(i);
                    }
                }
                return -1;
            }
        },
        getSize(): number {
            return this.names.length;
        }
    }
});

export default tabsStore;