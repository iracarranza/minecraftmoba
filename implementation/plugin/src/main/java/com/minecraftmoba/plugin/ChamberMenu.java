package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The hotbar as a menu the tester descends, and what each slot means right now.
 *
 * <h2>Why a stack and not nine fixed tools</h2>
 *
 * A chamber has more verbs than a hotbar has slots -- objectives, renewables,
 * routes, structures, each with its own list -- so the hotbar has to be a
 * <b>view onto a menu</b> rather than a fixed loadout. Holding and using an
 * item descends into its page; the back item climbs out.
 *
 * All of that is ordinary state, so it lives here and is settled without a
 * server. {@link ChamberHotbar} does nothing but turn a {@link View} into items
 * and read the tester's slot back.
 *
 * <h2>Three rules this enforces rather than describes</h2>
 *
 * <ol>
 * <li><b>A page may not exceed its slots.</b> Nine slots, one of which is BACK,
 *     so eight entries. A ninth would be a tool the tester cannot reach and has
 *     no way to learn is missing, so it is refused at construction instead of
 *     silently dropped.</li>
 * <li><b>BACK is always the last slot.</b> Fixed position, so descending four
 *     pages deep does not move the way out.</li>
 * <li><b>Session verbs are not hotbar verbs.</b> "Return to lab" and "test as
 *     player" end or hand over the session, and a hotbar is a thing you scroll
 *     past by accident. {@link #page} refuses them, which is the misclick
 *     argument made into a failure rather than a convention.</li>
 * </ol>
 */
public final class ChamberMenu {

    /** Hotbar slots. The last is reserved for BACK. */
    public static final int SLOTS = 9;
    public static final int BACK_SLOT = SLOTS - 1;
    public static final int MAX_ENTRIES = SLOTS - 1;

    /**
     * Verbs that end or hand over the session, and so never appear on the
     * hotbar. They belong in the inventory, where acting on one costs an
     * explicit open and click.
     */
    public static final List<String> SESSION_VERBS = List.of("return", "playtest");

    public enum Kind { SUBMENU, ACTION, BACK }

    public record Item(String id, String label, Kind kind) {
        public Item {
            Objects.requireNonNull(id);
            Objects.requireNonNull(label);
            Objects.requireNonNull(kind);
        }
        public static Item submenu(String id, String label) { return new Item(id, label, Kind.SUBMENU); }
        public static Item action(String id, String label) { return new Item(id, label, Kind.ACTION); }
    }

    public record Page(String id, String title, List<Item> items) {
        public Page {
            Objects.requireNonNull(id);
            items = List.copyOf(items);
        }
    }

    /** What is possible for this tester at this moment, asked per item. */
    public interface Gate {
        /** Null when the item may be used; otherwise the reason it may not. */
        String refuse(String itemId);
        Gate OPEN = id -> null;
    }

    /** One hotbar's worth of slots. Null entries are empty. */
    public record View(String pageId, String title, List<Item> slots, List<String> refusals) {
        public Item at(int slot) {
            return slot < 0 || slot >= slots.size() ? null : slots.get(slot);
        }
        public String refusalAt(int slot) {
            return slot < 0 || slot >= refusals.size() ? null : refusals.get(slot);
        }
    }

    /** What choosing a slot did. */
    public record Choice(Kind kind, String itemId, String refusal) {
        public boolean refused() { return refusal != null; }
    }

    private static final Item BACK = new Item("back", "Go back", Kind.BACK);

    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final String rootId;
    private final List<String> stack = new ArrayList<>();

    public ChamberMenu(String rootId) {
        this.rootId = Objects.requireNonNull(rootId);
        stack.add(rootId);
    }

    /**
     * Declare a page.
     *
     * @throws IllegalArgumentException if it would not fit the hotbar, or if it
     *         puts a session verb on it.
     */
    public ChamberMenu page(String id, String title, Item... items) {
        if (items.length > MAX_ENTRIES)
            throw new IllegalArgumentException("Page '" + id + "' has " + items.length
                    + " entries; a hotbar holds " + MAX_ENTRIES + " plus the way back.");
        for (Item i : items)
            if (SESSION_VERBS.contains(i.id()))
                throw new IllegalArgumentException("'" + i.id()
                        + "' ends or hands over the session and does not belong on a hotbar.");
        pages.put(id, new Page(id, title, Arrays.asList(items)));
        return this;
    }

    public String pageId() { return stack.get(stack.size() - 1); }
    public int depth() { return stack.size(); }
    public boolean atRoot() { return stack.size() == 1; }

    /**
     * Descend to a page the tester did not choose.
     *
     * A placement offered for confirmation changes what the hotbar is for, and
     * the caller -- not the menu -- is the thing that knows a proposal now
     * exists. BACK climbs out of it exactly as it climbs out of a chosen page,
     * so the tester is not given a second way to leave.
     */
    public void open(String pageId) {
        if (!pages.containsKey(pageId)) throw new IllegalArgumentException("No such page: " + pageId);
        if (!pageId.equals(pageId())) stack.add(pageId);
    }

    /** Return to the root, discarding the descent. */
    public void reset() { stack.clear(); stack.add(rootId); }

    /**
     * The current page laid out across the hotbar, with each entry's refusal
     * resolved now.
     *
     * Refusals are carried rather than entries being dropped: a tester who
     * cannot undo should see <i>Undo</i> greyed with a reason, not a hotbar
     * whose items move under them as history changes.
     */
    public View view(Gate gate) {
        Page page = pages.get(pageId());
        if (page == null) throw new IllegalStateException("No such page: " + pageId());
        var slots = new ArrayList<Item>(SLOTS);
        var refusals = new ArrayList<String>(SLOTS);
        for (int i = 0; i < SLOTS; i++) { slots.add(null); refusals.add(null); }
        for (int i = 0; i < page.items().size(); i++) {
            Item item = page.items().get(i);
            slots.set(i, item);
            refusals.set(i, gate.refuse(item.id()));
        }
        if (!atRoot()) slots.set(BACK_SLOT, BACK);
        return new View(page.id(), page.title(), slots, refusals);
    }

    /**
     * Use the item in a slot.
     *
     * A SUBMENU descends immediately; an ACTION is reported to the caller,
     * which owns the verb. A refused item does nothing and says why -- a
     * silent no-op reads as a broken button.
     */
    public Choice choose(int slot, Gate gate) {
        View v = view(gate);
        Item item = v.at(slot);
        if (item == null) return new Choice(null, null, null);
        if (item.kind() == Kind.BACK) {
            if (!atRoot()) stack.remove(stack.size() - 1);
            return new Choice(Kind.BACK, item.id(), null);
        }
        String refusal = v.refusalAt(slot);
        if (refusal != null) return new Choice(item.kind(), item.id(), refusal);
        if (item.kind() == Kind.SUBMENU) {
            if (!pages.containsKey(item.id()))
                throw new IllegalStateException("Submenu '" + item.id() + "' was never declared.");
            stack.add(item.id());
        }
        return new Choice(item.kind(), item.id(), null);
    }

    /**
     * The chamber's own menu.
     *
     * Named ids, because the hotbar is a view and the verbs are the contract:
     * {@link ChamberHotbar} renders this and the command layer dispatches it,
     * and neither should be matching on labels.
     */
    public static ChamberMenu chamberMenu() {
        return new ChamberMenu("root")
                .page("root", "Chamber",
                        Item.submenu("objective", "Place objective"),
                        Item.submenu("renewable", "Place renewable"),
                        Item.submenu("route", "Draw route"),
                        Item.action("regenerate", "Regenerate terrain"),
                        Item.action("undo", "Undo"),
                        Item.action("redo", "Redo"))
                .page("objective", "Objective",
                        Item.action("fountain", "Fountain"),
                        Item.action("outpost", "Outpost"),
                        Item.action("rampart", "Rampart"),
                        Item.action("spike", "Spike"))
                .page("renewable", "Renewable",
                        Item.action("renewable.place", "Place here"))
                .page("route", "Route",
                        Item.action("route.from", "Set start"),
                        Item.action("route.to", "Set end"),
                        Item.action("route.draw", "Draw"))
                .page("pending", "Pending placement",
                        Item.action("preview", "Show again"),
                        Item.action("confirm", "Place"),
                        Item.action("cancel", "Discard"));
    }
}
