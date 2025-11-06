package org.lime.fastmapper;

import org.lime.core.paper.BasePaperInstanceModule;
import org.lime.core.paper.BasePaperPlugin;

public class FastMapperPlugin
        extends BasePaperPlugin {
    @Override
    protected BasePaperInstanceModule<Instance> createModule(Instance instance) {
        return new BasePaperInstanceModule<>(instance);
    }
}
