package common.models;

import common.SlotRange;

import java.util.Optional;

public interface BaseModel {
    Optional<SlotRange> range();
}
