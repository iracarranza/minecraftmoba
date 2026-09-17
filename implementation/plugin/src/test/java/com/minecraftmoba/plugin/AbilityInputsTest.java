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
    @Test void fiftyDuplicateClickPairsResolveOnceAndTimeoutClearsMode() {
        var plugin=mock(MobaPlugin.class); var player=mock(Player.class); var scheduler=mock(BukkitScheduler.class);
        var config=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
        when(plugin.getConfig()).thenReturn(config); when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        var id=UUID.randomUUID(); var data=new PlayerData(id); data.classId="test";
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
                inputs.input(player,AbilityInputs.Input.LEFT_CLICK);
                // Reset expiry as a real sequence of mode exit/entry would.
                inputs.input(player,AbilityInputs.Input.SWAP_HAND);
                for(int t=0;t<config.getInt("abilities.definitions.lunge.cooldownTicks");t++) runnable.getValue().run();
                inputs.input(player,AbilityInputs.Input.SWAP_HAND);
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
