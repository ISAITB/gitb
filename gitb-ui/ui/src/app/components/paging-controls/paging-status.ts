/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import {Constants} from '../../common/constants';

export class PagingStatus {

  pageSize: number;
  currentPage: number;
  totalCount: number;
  previousDisabled: boolean;
  nextDisabled: boolean;

  constructor(init?: Partial<PagingStatus>) {
    if (init?.pageSize == undefined) this.pageSize = Constants.TABLE_PAGE_SIZE; else this.pageSize = init.pageSize
    if (init?.currentPage == undefined) this.currentPage = 1; else this.currentPage = init.currentPage
    if (init?.totalCount == undefined) this.totalCount = 0; else this.totalCount = init.totalCount
    if (init?.previousDisabled == undefined) this.previousDisabled = true; else this.previousDisabled = init.previousDisabled
    if (init?.nextDisabled == undefined) this.nextDisabled = true; else this.nextDisabled = init.nextDisabled
  }

  getLastPage() {
    return Math.ceil(this.totalCount / this.pageSize);
  }

}
