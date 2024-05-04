// License: GPL. For details, see LICENSE file.
package PerpendicularWayFromNode;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * Given a node and a reference way, this plugin can create a perpendicular way which starts at the given node.
 */
public class PerpendicularWayFromNodePlugin extends Plugin {
    public PerpendicularWayFromNodePlugin(PluginInformation info) {
        super(info);
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, new PerpendicularWayFromNodeAction());
    }
}
