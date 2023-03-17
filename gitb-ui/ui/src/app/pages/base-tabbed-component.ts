import { Component, ViewChild } from "@angular/core";
import { Router } from "@angular/router";
import { TabsetComponent } from "ngx-bootstrap/tabs";
import { BaseComponent } from "./base-component.component";

@Component({ template: '' })
export abstract class BaseTabbedComponent extends BaseComponent {

    @ViewChild('tabs', { static: false }) tabs?: TabsetComponent;
    tabIndexToShow = 0

    constructor(router: Router) {
        super()
        const tabParam = router.getCurrentNavigation()?.extras?.state?.tab
        if (tabParam != undefined) {
            this.tabIndexToShow = tabParam as number
        }
    }

    abstract loadTab(tabIndex: number): void

    showTab(tabIndex?: number) {
        setTimeout(() => {
            let tabToShow = tabIndex
            if (tabToShow == undefined) {
                tabToShow = this.tabIndexToShow
            }
            this.loadTab(tabToShow)
            if (this.tabs) {
                this.tabs.tabs[tabToShow].active = true
            }
        })
    }

}