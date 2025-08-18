package common.models;

import common.SlotRange;

import java.util.Optional;

public interface BaseDisplayModel
        extends BaseModel {
    @Override
    default Optional<SlotRange> range() {
        return Optional.empty();
    }
}
