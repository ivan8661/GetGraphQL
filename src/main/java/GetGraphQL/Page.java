package GetGraphQL;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


public class Page implements Pageable {
    private int limit;
    private int offset;
    private Sort sort;

    public Page(int limit, int offset, Sort sort) {
        this.limit = limit;
        this.offset = offset;
        this.sort = sort;
    }

    public Page(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    public int getPageNumber() {
        return offset / limit;
    }
    @Override
    public int getPageSize() {
        return limit;
    }
    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new Page(getPageSize(), (int) (getOffset() + getPageSize()));
    }
    public Pageable previous() {
        return hasPrevious() ?
                new Page(getPageSize(), (int) (getOffset() - getPageSize())): this;
    }
    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }
    @Override
    public Pageable first() {
        return new Page(getPageSize(), 0);
    }

    @Override
    public Pageable withPage(int i) {
        return null;
    }

    @Override
    public boolean hasPrevious() {
        return offset > limit;
    }
}
