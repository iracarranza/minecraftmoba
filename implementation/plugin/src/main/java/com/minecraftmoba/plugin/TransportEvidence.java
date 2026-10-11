package com.minecraftmoba.plugin;

import org.bukkit.Location;
import java.util.*;
import java.util.function.Consumer;

/** Class creates delivery; shared infrastructure observes it. No line identity is sent by a carrier. */
final class TransportEvidence {
    record Delivery(UUID owner,UUID method,Location source,Location destination,int ratedSlots,int deliveredSlots,
                    long departed,long arrived,boolean temporary) {}
    private final List<Consumer<Delivery>> observers=new ArrayList<>();
    void observe(Consumer<Delivery> observer){observers.add(observer);}
    void delivered(Delivery delivery){for(var observer:List.copyOf(observers))observer.accept(delivery);}
}
