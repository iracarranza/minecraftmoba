package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CompendiumsTest {
    @Test void nonParticipantsAlwaysReceiveGuidesIncludingUnenrolledVisitors() {
        for(var state:Match.State.values())assertTrue(Compendiums.shouldIssue(state,false));
    }
    @Test void guidesAreRemovedDuringDraftAndPlayAndReturnAfterReset() {
        for(var state:Match.State.values())assertEquals(state==Match.State.IDLE,Compendiums.shouldIssue(state,true));
    }
    @Test void freeSlotNeverDisplacesItemsAndPreservesConfiguredCastToggleSlot() {
        var empty=new boolean[36];assertEquals(-1,Compendiums.freeSlot(empty,6));
        empty[8]=true;assertEquals(-1,Compendiums.freeSlot(empty,6));
        empty[6]=true;assertEquals(6,Compendiums.freeSlot(empty,6));
        assertEquals(8,Compendiums.freeSlot(empty,6,6));
    }
    private static List<ClickEvent> clicks(Component component) {
        var events=new ArrayList<ClickEvent>();
        if(component.clickEvent()!=null)events.add(component.clickEvent());
        for(var child:component.children())events.addAll(clicks(child));return events;
    }
    @Test void everyContentsLinkPointsToAnExistingPage() {
        var pages=CompendiumContent.debugPages();assertTrue(pages.size()<100);
        for(var page:pages)for(var event:clicks(page))if(event.action()==ClickEvent.Action.CHANGE_PAGE) {
            int target=Integer.parseInt(event.value());assertTrue(target>=1&&target<=pages.size());
        }
    }
    @Test void everyDebugCommandAppearsExactlyOnceAndMutationsRequireEditing() {
        var pages=CompendiumContent.debugPages();var entries=CompendiumContent.sections().values().stream().flatMap(List::stream).toList();
        var actual=pages.stream().flatMap(p->clicks(p).stream()).filter(e->e.action()!=ClickEvent.Action.CHANGE_PAGE).toList();
        assertEquals(entries.size(),actual.size());
        for(var e:entries)assertEquals(1,actual.stream().filter(a->a.value().equals(e.command())).count());
        for(var e:actual)if(e.value().contains("apply")||e.value().contains("undo")||e.value().contains("reset")||e.value().contains("<"))
            assertEquals(ClickEvent.Action.SUGGEST_COMMAND,e.action(),e.value());
    }
    @Test void playerBookOffersExistingCastPreferencesWithoutSuggestingDangerousCommands() {
        var clicks=CompendiumContent.playerPages().stream().flatMap(p->clicks(p).stream()).toList();
        for(String mode:List.of("quick","hold","double"))assertTrue(clicks.stream().anyMatch(c->c.value().equals("/moba settings cast "+mode)));
        assertFalse(clicks.stream().anyMatch(c->c.value().contains("reset")||c.value().contains("lab author")));
    }
}
