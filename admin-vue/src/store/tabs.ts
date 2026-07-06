/**
 * store.ts
 * create by Snake as 2026-07-04
 * @description:
 */

import {defineStore} from "pinia";

export interface TabCard {
    id: number;
    name: string;
    title: string;
    path: string;
    closable: boolean;
}

const homeTabCard: TabCard = {
    id: 0,
    name: 'HomeView',
    title: '首页',
    path: '/home',
    closable: false,
}

interface TabsStore {
    names: string[];
    cards: TabCard[];
}

export const useTabsStore = defineStore("tabs", {
    state: (): TabsStore => {
        const value = sessionStorage.getItem("tab_cards");
        if (!value || value === '') {
            return {
                names: [homeTabCard.name],
                cards: [homeTabCard],
            };
        } else {
            const obj = JSON.parse(value);
            return {
                names: obj.names,
                cards: obj.cards,
            }
        }
    },
    actions: {
        addCard(card: TabCard): void {
            this.names.push(card.name);
            this.cards.push(card);
            sessionStorage.setItem("tab_cards", JSON.stringify({
                names: this.names,
                cards: this.cards,
            }));
        },
        removeCard(index: number): void {
            this.names.splice(index, 1);
            this.cards.splice(index, 1);
            sessionStorage.setItem("tab_cards", JSON.stringify({
                names: this.names,
                cards: this.cards,
            }));
        },
        clear(): void {
            sessionStorage.removeItem("tab_cards");
            this.$reset();
        }
    },
    getters: {
        getNames() {
            return (): string[] => {
                return this.names;
            }
        },
        getCards() {
            return (): TabCard[] => {
                return this.cards;
            }
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
        getSize() {
            return (): number => {
                return this.names.length;
            }
        }
    }
});