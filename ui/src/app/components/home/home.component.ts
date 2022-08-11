import {Component, OnInit, ViewChild} from '@angular/core';
import {RestService} from '../../services/rest.service';
import {Router} from '@angular/router';
import {AlertComponent} from '../alert/alert.component';
import {ClarityIcons, trashIcon} from '@cds/core/icon';
import {OpenOrder} from "../../models/open-order";
import {ClrDatagridStateInterface} from "@clr/angular";
import {OpenOrderPage} from "../../models/open-order-page";
import {debounceTime, Subject} from "rxjs";
import {DatePipe} from '@angular/common';
import {SettledOrderPage} from "../../models/settled-order-page";
import {SettledOrder} from "../../models/settled-order";

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: []
})
export class HomeComponent implements OnInit {


  // @ts-ignore
  @ViewChild(AlertComponent, {static: true}) private alert: AlertComponent;

  openOrderTableState: ClrDatagridStateInterface = {page: {current: 1, from: 1, size: 10, to: 10}};
  openOrderPage: OpenOrderPage = new OpenOrderPage();
  openOrder: OpenOrder = new OpenOrder();
  openOrderItems: OpenOrder[] = [];
  openOrderLoading = false;
  openOrderpage: number = 1;
  openOrderTotal: number = 1;
  openOrderDebouncer: Subject<any> = new Subject<any>();

  settledOrderTableState: ClrDatagridStateInterface = {page: {current: 1, from: 1, size: 10, to: 10}};
  settledOrderPage: SettledOrderPage = new SettledOrderPage();
  settledOrderItems: SettledOrder[] = [];
  settledOrderLoading = false;
  settledOrderpage: number = 1;
  settledOrderTotal: number = 1;
  settledOrderDebouncer: Subject<any> = new Subject<any>();

  constructor(private restService: RestService, private router: Router, private datePipe: DatePipe) {
    ClarityIcons.addIcons(trashIcon);
    this.openOrder.ticker = "GOOGL";
    this.openOrder.type = "BUY";
  }

  ngOnInit(): void {
    this.openOrderLoading = true;
    this.openOrderDebouncer
      .pipe(debounceTime(700))
      .subscribe(state => {
          this.openOrderTableState = state;
          this.openOrderLoading = true;
          if (!state.page) {
            state.page = {
              from: 1,
              to: 10,
              size: 10,
            };
          }
          // @ts-ignore
          let pageStart = state.page.current - 1;
          let pageSize = state.page.size;
          this.restService.getOpenOrders(pageStart, pageSize, state.filters, state.sort).subscribe(data => {
              this.openOrderPage = data;
              this.openOrderTotal = this.openOrderPage?.totalElements || 1;
              this.openOrderLoading = false;
            },
            error => {
              this.openOrderLoading = false;
            });
        }
      );
    this.settledOrderDebouncer
      .pipe(debounceTime(700))
      .subscribe(state => {
          this.settledOrderTableState = state;
          this.settledOrderLoading = true;
          if (!state.page) {
            state.page = {
              from: 1,
              to: 10,
              size: 10,
            };
          }
          // @ts-ignore
          let pageStart = state.page.current - 1;
          let pageSize = state.page.size;
          this.restService.getSettledOrders(pageStart, pageSize, state.filters, state.sort).subscribe(data => {
              this.settledOrderPage = data;
              this.settledOrderTotal = this.settledOrderPage?.totalElements || 1;
              this.settledOrderLoading = false;
            },
            error => {
              this.settledOrderLoading = false;
            });
        }
      );
  }

  refreshOpenOrder(state: ClrDatagridStateInterface) {
    this.openOrderDebouncer.next(state);
  }

  refreshSettledOrder(state: ClrDatagridStateInterface) {
    this.settledOrderDebouncer.next(state);
  }

  placeOrder(): void {
    console.log('save order!');
    if (this.openOrder.price < 0 || this.openOrder.quantity < 0) {
      return;
    }
    this.restService.placeOrder(this.openOrder)
      .subscribe(data => {
        this.alert.showSuccess('Order placed successfully!');
        this.refreshOpenOrder(this.openOrderTableState)
        this.refreshSettledOrder(this.settledOrderTableState)
      }, error => {
        this.alert.showError('Order failed!');
      });
  }
}
