/**
 * 공통 페이징 컴포넌트
 * 모든 목록 페이지에서 재사용 가능
 */
class PaginationManager {
    constructor(containerId, {
        onPageChange = () => {
        }, onPageSizeChange = () => {
        }
    } = {}) {
        this.containerId = containerId;
        this.container = document.getElementById(containerId);
        this.currentPage = 0;
        this.pageSize = 5;
        this.totalElements = 0;
        this.totalPages = 0;
        this.onPageChange = onPageChange;
        this.onPageSizeChange = onPageSizeChange;

        this.init();
    }

    init() {
        if (!this.container) {
            console.error("Pagination container not found:", this.containerId);
            return;
        }

        this.bindEvents();
    }

    bindEvents() {
        // 페이지 이동 버튼들
        const firstPageBtn = this.container.querySelector("#firstPageBtn");
        const prevPageBtn = this.container.querySelector("#prevPageBtn");
        const nextPageBtn = this.container.querySelector("#nextPageBtn");
        const lastPageBtn = this.container.querySelector("#lastPageBtn");
        const pageSizeSelect = this.container.querySelector("#pageSizeSelect");

        if (firstPageBtn)
            firstPageBtn.addEventListener("click", () => this.goToPage(0));
        if (prevPageBtn)
            prevPageBtn.addEventListener("click", () =>
                this.goToPage(this.currentPage - 1)
            );
        if (nextPageBtn)
            nextPageBtn.addEventListener("click", () =>
                this.goToPage(this.currentPage + 1)
            );
        if (lastPageBtn)
            lastPageBtn.addEventListener("click", () =>
                this.goToPage(this.totalPages - 1)
            );
        if (pageSizeSelect)
            pageSizeSelect.addEventListener("change", (e) =>
                this.changePageSize(parseInt(e.target.value))
            );
    }

    updatePagination(pageData) {
        this.currentPage = pageData.number;
        this.pageSize = pageData.size;
        this.totalElements = pageData.totalElements;
        this.totalPages = pageData.totalPages;
        this.updateUI();
        if (this.totalPages > 0) {
            this.show();
        } else {
            this.hide();
        }
    }

    updateUI() {
        // 페이지 정보 업데이트
        const paginationInfo = this.container.querySelector("#paginationInfo");
        const currentPageInfo = this.container.querySelector("#currentPageInfo");
        const pageSizeSelect = this.container.querySelector("#pageSizeSelect");

        if (paginationInfo) {
            paginationInfo.textContent = `총 ${this.totalElements}개 항목`;
        }

        if (currentPageInfo) {
            currentPageInfo.textContent = `${this.currentPage + 1} / ${
                this.totalPages
            }`;
        }

        if (pageSizeSelect) {
            pageSizeSelect.value = this.pageSize;
        }

        // 버튼 상태 업데이트
        this.updateButtonStates();
    }

    updateButtonStates() {
        const firstPageBtn = this.container.querySelector("#firstPageBtn");
        const prevPageBtn = this.container.querySelector("#prevPageBtn");
        const nextPageBtn = this.container.querySelector("#nextPageBtn");
        const lastPageBtn = this.container.querySelector("#lastPageBtn");

        const isFirstPage = this.currentPage === 0;
        const isLastPage = this.currentPage === this.totalPages - 1;

        if (firstPageBtn) firstPageBtn.disabled = isFirstPage;
        if (prevPageBtn) prevPageBtn.disabled = isFirstPage;
        if (nextPageBtn) nextPageBtn.disabled = isLastPage;
        if (lastPageBtn) lastPageBtn.disabled = isLastPage;
    }

    goToPage(page) {
        if (page < 0 || page >= this.totalPages) return;

        this.currentPage = page;
        this.onPageChange(page);
    }

    changePageSize(newSize) {
        this.pageSize = newSize;
        this.currentPage = 0; // 페이지 크기 변경 시 첫 페이지로 이동
        this.onPageSizeChange();
    }

    reset() {
        this.goToPage(0);
    }

    show() {
        this.container.classList.remove('hidden');
    }

    hide() {
        this.container.classList.add('hidden');
    }

    getCurrentPage() {
        return this.currentPage;
    }

    getPageSize() {
        return this.pageSize;
    }
}

export {PaginationManager};
// 전역에서 사용할 수 있도록 등록
// window.PaginationManager = PaginationManager;
