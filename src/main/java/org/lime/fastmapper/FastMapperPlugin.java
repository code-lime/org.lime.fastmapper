package org.lime.fastmapper;

import org.lime.LimeCore;

public class FastMapperPlugin extends LimeCore {
    public static FastMapperPlugin _plugin;

    @Override
    public String getLogPrefix() {
        return "FastMapper";
    }
    @Override
    public String getConfigFile() {
        return "plugins/fastmapper/";
    }
}