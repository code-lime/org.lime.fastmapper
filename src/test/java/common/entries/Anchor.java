package common.entries;

public enum Anchor {
    Center(State.None, State.None),
    Left(State.False, State.None),
    Right(State.True, State.None),

    Top(State.None, State.False),
    TopLeft(State.False, State.False),
    TopRight(State.True, State.False),

    Bottom(State.None, State.True),
    BottomLeft(State.False, State.True),
    BottomRight(State.True, State.True);

    public final State right;
    public final State bottom;

    Anchor(State right, State bottom) {
        this.right = right;
        this.bottom = bottom;
    }
}
