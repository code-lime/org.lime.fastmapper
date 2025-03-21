package common.entries;

import common.Vector2i;

public interface OffsetEntry extends Entry {
    default Vector2i offset() {
        return new Vector2i();
    }

    String componentWithoutOffset(int x, int y, int sizeX, int sizeY);

    @Override
    default String component(int x, int y, int sizeX, int sizeY) {
        Vector2i offset = offset();
        return componentWithoutOffset(x + offset.x, y + offset.y, sizeX, sizeY);
    }
}
