package us.tryy3.spigot.plugins.minatsuminecraftalertsplugin;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by tryy3 on 2016-02-15.
 */
public class Alerts extends JavaPlugin {

    @Override
    public void onEnable() {
        new TCPClient(this).start();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
