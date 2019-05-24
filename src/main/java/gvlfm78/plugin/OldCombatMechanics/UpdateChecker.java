package gvlfm78.plugin.OldCombatMechanics;

import gvlfm78.plugin.OldCombatMechanics.updater.BukkitUpdateSource;
import gvlfm78.plugin.OldCombatMechanics.updater.ModuleUpdateChecker;
import gvlfm78.plugin.OldCombatMechanics.updater.SpigotUpdateSource;
import gvlfm78.plugin.OldCombatMechanics.updater.UpdateSource;
import gvlfm78.plugin.OldCombatMechanics.utilities.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class UpdateChecker {
    private UpdateSource updateSource;

    public UpdateChecker(OCMMain plugin, File pluginFile){
        switch(ModuleUpdateChecker.getMode()){
            case "spigot":
                this.updateSource = new SpigotUpdateSource();
                break;
            case "bukkit":
                this.updateSource = new BukkitUpdateSource(plugin, pluginFile);
                break;
            case "auto":
                String serverVersion = Bukkit.getVersion().toLowerCase(Locale.ROOT);
                if(serverVersion.contains("spigot") || serverVersion.contains("paper")){
                    this.updateSource = new SpigotUpdateSource();
                } else {
                    this.updateSource = new BukkitUpdateSource(plugin, pluginFile);
                }
        }
    }

    public void sendUpdateMessages(CommandSender sender){
        if(sender instanceof Player){
            sendUpdateMessages(((Player) sender)::sendMessage);
        } else {
            sendUpdateMessages(Messenger::info);
        }
    }

    private void sendUpdateMessages(Consumer<String> target){//Sends messages to a player
        updateSource.getUpdateMessages().stream()
                .filter(Objects::nonNull)
                .filter(message -> !message.isEmpty())
                .forEach(target);
    }
}