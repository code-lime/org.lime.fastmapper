package common;

import java.util.Map;

public interface FontData {
    String key();
    int height();
    Map<Character, Integer> charWidth();
    default int defaultWidth() {
        return 5;
    }
}
