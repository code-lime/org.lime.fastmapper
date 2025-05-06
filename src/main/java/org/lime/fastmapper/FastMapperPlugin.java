package org.lime.fastmapper;

import org.lime.core.paper.CoreInstancePlugin;

public class FastMapperPlugin extends CoreInstancePlugin {
    public static FastMapperPlugin instance;

    @Override
    public String logPrefix() {
        return "FastMapper";
    }
    @Override
    public String configFile() {
        return "plugins/fastmapper/";
    }
}
