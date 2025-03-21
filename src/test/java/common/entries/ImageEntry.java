package common.entries;

import common.RgbColor;

public interface ImageEntry extends OffsetEntry {
    String DefaultFont = "ico";

    default String font() {
        return DefaultFont;
    }
    default RgbColor color() {
        return new RgbColor(1,1,1);
    }

    int symbol();
    int width();

    @Override
    default String componentWithoutOffset(int x, int y, int sizeX, int sizeY) {
        return String.format("%s %s %s %s %s %s %s %s", font(), color(), symbol(), width(), x, y, sizeX, sizeY);
    }
}
