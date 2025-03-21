package common.entries;

public interface RawEntry extends Entry {
    String component();
    default String component(int x, int y, int sizeX, int sizeY) {
        return component();
    }
}
