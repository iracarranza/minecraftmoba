package com.minecraftmoba.plugin;

import java.util.*;

/** One ordinary worker has one allocation. Temporary workers never enter this ledger. */
final class WorkforceAllocation {
    record Worker(UUID id,UUID owner,int slots,String commitment) {}
    private final Map<UUID,Worker> workers=new LinkedHashMap<>();
    boolean add(UUID id,UUID owner,int slots,int limit){
        if(slots<1 || workers.containsKey(id) || count(owner)>=limit)return false;
        workers.put(id,new Worker(id,owner,slots,null));return true;
    }
    int count(UUID owner){return (int)workers.values().stream().filter(w->w.owner().equals(owner)).count();}
    int active(UUID owner){return (int)workers.values().stream().filter(w->w.owner().equals(owner)&&w.commitment()==null).count();}
    int capacity(String line){return workers.values().stream().filter(w->line.equals(w.commitment())).mapToInt(Worker::slots).sum();}
    boolean commit(UUID id,String line){var w=workers.get(id);if(w==null || w.commitment()!=null || line==null)return false;workers.put(id,new Worker(id,w.owner(),w.slots(),line));return true;}
    boolean withdraw(UUID id){var w=workers.get(id);if(w==null || w.commitment()==null)return false;workers.put(id,new Worker(id,w.owner(),w.slots(),null));return true;}
    void remove(UUID id){workers.remove(id);}
    Worker get(UUID id){return workers.get(id);}
    List<Worker> workers(){return List.copyOf(workers.values());}
}
