package common.entries;

import common.RgbColor;

import java.util.Collection;
import java.util.Optional;

public interface TextEntry extends OffsetEntry {
    default String font() {
        return "mini";
    }
    default RgbColor color() {
        return new RgbColor(1,1,1);
    }
    default Optional<RgbColor> shadow() {
        return Optional.empty();
    }

    default int scrollLine() {
        return 0;
    }

    default Anchor anchor() {
        return Anchor.TopLeft;
    }

    default Optional<Integer> maxWidth() {
        return Optional.empty();
    }
    default Optional<Integer> maxLines() {
        return Optional.empty();
    }

    Collection<String> lines();

    @Override
    default String componentWithoutOffset(int x, int y, int sizeX, int sizeY) {
        return String.join("\n", lines());
    }
}
