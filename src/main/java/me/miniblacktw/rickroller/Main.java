package me.miniblacktw.rickroller;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private File songFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        songFile = new File(getDataFolder(), "rickroll.nbs");
        downloadSong();
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null || !Bukkit.getPluginManager().getPlugin("Citizens").isEnabled()) {
            getLogger().severe("§cCitizens not found");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("NoteBlockAPI") == null) {
            getLogger().severe("§cNoteBlockAPI not found");
        }
        getCommand("rickroll").setExecutor(this);
    }

    private void downloadSong() {
        if (!songFile.exists()) {
            getDataFolder().mkdirs();

            String url = "https://github.com/miniblackTW/miniblackTW/blob/main/rickroll.nbs";
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, songFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("§c7Downloaded rickroll.nbs");
            } catch (Exception e) {
                getLogger().warning("§7Failed to download rickroll.nbs: §c" + e.getMessage());
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) return false;
        Player t = Bukkit.getPlayer(args[0]);
        if (t == null) {
            sender.sendMessage("§7Player not found");
            return true;
        }
        rickroll(t);
        sender.sendMessage("§b" + t.getName() + " §7is being rick rolled");
        return true;
    }

    private void rickroll(Player p) {
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "§6§lRick Astley");
        SkinTrait trait = npc.getOrAddTrait(SkinTrait.class);
        trait.setSkinName("Niblesniff");
        Location spawn = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(3));
        npc.spawn(spawn);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (npc.isSpawned()) {

                    trait.setSkinName("Niblesniff");
                }
            }
        }.runTaskLater(this, 2L);
        RadioSongPlayer sp = null;
        if (songFile != null && songFile.exists()) {
            try {
                Song song = NBSDecoder.parse(songFile);
                sp = new RadioSongPlayer(song);
                sp.addPlayer(p);
                sp.setPlaying(true);
            } catch (Exception e) {
                getLogger().warning("§7Couldn't play rickroll.nbs: §c" + e.getMessage());
            }
        }

        final RadioSongPlayer fsp = sp;
        new BukkitRunnable() {
            double ticks = 0;
            @Override
            public void run() {

                if (!p.isOnline() || !npc.isSpawned() || (fsp != null && !fsp.isPlaying())) {
                    npc.destroy();
                    if (fsp != null) fsp.setPlaying(false);
                    this.cancel();
                    return;
                }

                ticks += 1.0;
                Location pLoc = p.getLocation();
                Vector forward = pLoc.getDirection().setY(0).normalize();
                Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
                double offset = Math.sin(ticks * 0.2) * 0.5;
                Location tLoc = pLoc.clone()
                        .add(forward.multiply(3))
                        .add(right.multiply(offset));
                float yaw = pLoc.getYaw() + 180;
                float swing = (float) (Math.sin(ticks * 0.5) * 30.0);
                tLoc.setYaw(yaw + swing);
                tLoc.setPitch(0);
                npc.teleport(tLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }.runTaskTimer(this, 0L, 1L);
    }
}