package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.io.InputStreamReader;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbilityInputsTest {
    /**
     * One arming buys one cast. Casting m1 disarms immediately, so m2 or q
     * needs a fresh arming rather than chaining off the same one.
     */
    @Test void fiftyDuplicateClickPairsResolveOnceAndTimeoutClearsMode() throws Exception {
        var plugin=mock(MobaPlugin.class); var player=mock(Player.class); var scheduler=mock(BukkitScheduler.class);
        var defaults=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
        var config=new YamlConfiguration();
        config.loadFromString("abilities:\n  modeTimeoutTicks: 50\n");
        config.setDefaults(defaults); // existing scaffold config lacks the later registry sections
        Settings.load(config);
        when(plugin.getConfig()).thenReturn(config); when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        var id=UUID.randomUUID(); var data=new PlayerData(id); data.classId="test"; data.level=1;
        when(player.getUniqueId()).thenReturn(id); when(player.getName()).thenReturn("test");
        when(plugin.enrolled(player)).thenReturn(true); when(plugin.data(player)).thenReturn(data);
        when(player.getLocation()).thenReturn(new Location(mock(World.class),0,0,0));
        when(player.getVelocity()).thenAnswer(invocation->new Vector());
        try(var bukkit=mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
            var inputs=new AbilityInputs(plugin,mock(Provenance.class));
            var runnable=ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler).runTaskTimer(eq(plugin),runnable.capture(),eq(1L),eq(1L));
            inputs.input(player,AbilityInputs.Input.SWAP_HAND);
            for(int i=0;i<50;i++) {
                inputs.input(player,AbilityInputs.Input.LEFT_CLICK);
                // One arming buys one cast: the second click finds the player
                // already disarmed and must not fire again.
                assertFalse(data.modeState.active, "casting must disarm immediately");
                inputs.input(player,AbilityInputs.Input.LEFT_CLICK);
                for(int t=0;t<config.getInt("abilities.definitions.lunge.cooldownTicks");t++) runnable.getValue().run();
                inputs.input(player,AbilityInputs.Input.SWAP_HAND);   // re-arm
            }
            verify(player,times(50)).setVelocity(any());
            verify(player,never()).getInventory();
            for(int t=0;t<=config.getInt("abilities.modeTimeoutTicks");t++) runnable.getValue().run();
            assertFalse(data.modeState.active);
            inputs.input(player,AbilityInputs.Input.LEFT_CLICK);
            verify(player,times(50)).setVelocity(any());
        }
    }
}
