import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {OpenOrder} from '../models/open-order';
import {OpenOrderPage} from "../models/open-order-page";
import {SettledOrderPage} from "../models/settled-order-page";

@Injectable({
  providedIn: 'root'
})
export class RestService {

  constructor(private http: HttpClient) {
  }

  public getOpenOrders(page: number, size: number | undefined, filters: any, sort: any): Observable<OpenOrderPage> {
    let url = `/api/open-order?page=${page}&size=${size}&sort=id,desc`;
    return this.http.get<OpenOrderPage>(url);
  }

  public getSettledOrders(page: number, size: number | undefined, filters: any, sort: any): Observable<SettledOrderPage> {
    let url = `/api/settled-order?page=${page}&size=${size}&sort=id,desc`;
    return this.http.get<SettledOrderPage>(url);
  }

  public placeOrder(openOrder: OpenOrder): Observable<any> {
    return this.http.post('/api/order', openOrder);
  }

}
