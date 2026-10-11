package com.minecraftmoba.plugin;

import java.util.*;

/** Rated Capacity is supplied by the method; two real deliveries prove repeatability and the pulse interval. */
final class TransportProof {
    record Delivery(UUID method,int ratedSlots,int deliveredSlots,long departed,long arrived,boolean temporary){}
    record Proof(UUID method,int slots,long transitTicks,long intervalTicks){}
    private final Map<UUID,Delivery> previous=new HashMap<>();
    Proof observe(Delivery delivery){
        if(delivery.temporary() || delivery.ratedSlots()<1 || delivery.deliveredSlots()!=delivery.ratedSlots() || delivery.arrived()<=delivery.departed())return null;
        Delivery prior=previous.put(delivery.method(),delivery);
        if(prior==null || prior.ratedSlots()!=delivery.ratedSlots() || delivery.departed()<=prior.arrived())return null;
        return new Proof(delivery.method(),delivery.ratedSlots(),delivery.arrived()-delivery.departed(),delivery.arrived()-prior.arrived());
    }
}
