package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.*;

/** Book contents are independent of inventory distribution and command permissions. */
final class CompendiumContent {
    record Entry(String command, boolean run) {
        Component link() {
            String label=command.substring("/moba ".length());
            int parameter=label.indexOf('<'); if(parameter>=0) label=label.substring(0,parameter).trim()+" …";
            return Component.text(label,NamedTextColor.DARK_BLUE)
                    .clickEvent(run?ClickEvent.runCommand(command):ClickEvent.suggestCommand(command))
                    .hoverEvent(HoverEvent.showText(Component.text((run?"Run: ":"Edit/confirm: ")+command)));
        }
    }
    private static final Set<String> RUN = Set.of("/moba compendium player","/moba compendium debug",
            "/moba join","/moba catalogue","/moba rewards","/moba lab maps","/moba lab classes");
    static List<Component> playerPages() {
        return List.of(
                Component.text("Player Compendium\n\nA basic field guide.\n\n",NamedTextColor.BLACK)
                        .append(new Entry("/moba join",true).link()).append(Component.text("\n"))
                        .append(new Entry("/moba catalogue",true).link()).append(Component.text("\n"))
                        .append(new Entry("/moba rewards",true).link()),
                Component.text("Casting\n\nQuick: press fires.\nHold: preview; release fires.\nDouble: preview; press again fires.\nAnother ability cancels aim.\n\nHold release is inferred from input silence.",NamedTextColor.BLACK),
                Component.text("Cast preference\n\nUse the comparator in hotbar slot 9 outside a match, or choose:\n\n",NamedTextColor.BLACK)
                        .append(new Entry("/moba settings cast quick",true).link()).append(Component.text("\n"))
                        .append(new Entry("/moba settings cast hold",true).link()).append(Component.text("\n"))
                        .append(new Entry("/moba settings cast double",true).link()),
                Component.text("Getting started\n\nAn admin opens a match and assigns teams. Follow the draft prompts.\n\nCast modes apply where the ability supports them. Your preference persists between matches.\n\nMore guidance will follow.",NamedTextColor.BLACK));
    }
    static Map<String,List<Entry>> sections() {
        var sections=new LinkedHashMap<String,List<Entry>>();
        add(sections,"Navigation", "compendium player\ncompendium debug\njoin\ncatalogue\nrewards\nsettings cast <quick|hold|double>\nrecall");
        add(sections,"Draft", "ban <class-id>\nhover <class-id>\npick <class-id>");
        add(sections,"Lab setup", "lab start\nlab menu\nlab maps\nlab classes\nlab map <id-or-number>\nlab class <id>\nlab level <n>\nlab team <north|south>\nlab play\nlab end\nlab leave\nlab status");
        add(sections,"Lab authoring", "lab author help\nlab author inspect\nlab author preview fountain\nlab author preview outpost rigid\nlab author preview outpost follow\nlab author preview bastion\nlab author preview spike\nlab author path from\nlab author path to\nlab author access\nlab author apply\nlab undo\nlab author cancel\nlab author swarm mountain_ravager\nlab author fauna <kind> [count]");
        add(sections,"Match", "match open\nmatch options\nmatch select <map-id>\nmatch start\nmatch classes-done\nmatch play\nmatch start-test\nmatch status\nmatch add <player> <north|south>\nmatch fountain <north|south> disable\nmatch kill <player>\nmatch skip <minutes>\nmatch siege <north|south> <outpost|bastion|spike> <combat|structural|signature|lair> [amount]\nmatch reset\nreset");
        add(sections,"Player tuning", "setclass <player> <class-id>\nsetlevel <player> <n>\nxp <player> <amount>\ngrant <player> <efficiency|yield|damage> <tier>\nsyncadv <player>\ntask <player>\nwork [player]\ndebug <player>\ninfra enter\ninfra exit\ninfra toggle\ninfra status");
        add(sections,"Worksites", "worksite status\nworksite list\nworksite capitalize <id> <north|south>\nworksite deplete <id>\nworksite exploit <id>");
        add(sections,"Renewables", "renew status\nrenew kinds\nrenew spawn <kind> [radius] [capacity]\nrenew capture <kind> [radius]\nrenew remove <id>\nrenewables");
        add(sections,"Contributions", "contrib options\ncontrib choose <form>\ncontrib capitalize <worksiteId> <team>\ncontrib allocate <team> <form>\ncontrib status");
        add(sections,"Infrastructure", "route\nroutes\nhub\nhub build\ntestbed routes\ntestbed supply\ntestbed development\ntestbed constructs\ntestbed all\nmaps\nbench copy\nbench dense [n]\nbench scattered [n]");
        add(sections,"HUD probes", "hudprobe all\nhudprobe off\nhudprobe <layer> [x]\nhudprobe clock <ascent|off>\nhudprobe notice <ascent|off|test>\nhudprobe numeral <ascent|off>\nhudprobe bars <ascent|off>");
        add(sections,"Diagnostics", "health\nregen\nslots\ndurability\nprovenance\nmaterials\ncurve [materials...]\nping <text>\npings\ndebug on\ndebug off\ndebug go colosseum");
        return sections;
    }
    private static void add(Map<String,List<Entry>> sections,String title,String commands) {
        sections.put(title,commands.lines().map(c->new Entry("/moba "+c,RUN.contains("/moba "+c))).toList());
    }
    static List<Component> debugPages() {
        var sections=sections();var pages=new ArrayList<Component>();
        pages.add(Component.empty());pages.add(Component.empty());
        var index=new ArrayList<Component>();
        for(var section:sections.entrySet()) {
            int first=pages.size()+1;
            index.add(Component.text(section.getKey(),NamedTextColor.DARK_BLUE)
                    .clickEvent(ClickEvent.changePage(first)));
            for(int offset=0;offset<section.getValue().size();offset+=2) {
                Component page=Component.text(section.getKey()+"\n\n",NamedTextColor.BLACK);
                for(int i=offset;i<Math.min(offset+2,section.getValue().size());i++)
                    page=page.append(section.getValue().get(i).link()).append(Component.text("\n\n"));
                page=page.append(Component.text("Contents",NamedTextColor.DARK_GREEN).clickEvent(ClickEvent.changePage(1)));
                pages.add(page);
            }
        }
        Component intro=Component.text("Debug Compendium\n\nClick a section:\n\n",NamedTextColor.BLACK);
        for(int i=0;i<6;i++)intro=intro.append(index.get(i)).append(Component.text("\n"));
        pages.set(0,intro.append(Component.text("\nMore sections →",NamedTextColor.DARK_GREEN).clickEvent(ClickEvent.changePage(2))));
        Component second=Component.text("Command reference\n\n",NamedTextColor.BLACK);
        for(int i=6;i<index.size();i++)second=second.append(index.get(i)).append(Component.text("\n"));
        pages.set(1,second.append(Component.text("\nLinks suggest commands.\nPermissions apply.")));
        return List.copyOf(pages);
    }
}
