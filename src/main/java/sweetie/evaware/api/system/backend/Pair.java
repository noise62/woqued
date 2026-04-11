package sweetie.evaware.api.system.backend;

public class Pair<A, B> {
    private final A left;
    private final B right;

    public Pair(A left, B right) {
        this.left = left;
        this.right = right;
    }

    public A left() {
        return left;
    }

    public B right() {
        return right;
    }
}