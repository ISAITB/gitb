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
