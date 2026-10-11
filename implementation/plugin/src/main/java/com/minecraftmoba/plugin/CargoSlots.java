package com.minecraftmoba.plugin;

import org.bukkit.inventory.*;
import java.util.*;

/** Real item stacks packed by ordinary per-item stack size, never by item count as Capacity. */
final class CargoSlots {
    private CargoSlots(){}
    static boolean eligible(ItemStack item){
        return item!=null && !item.isEmpty() && item.getItemMeta().getPersistentDataContainer().getKeys().stream()
                .noneMatch(key->key.getNamespace().equals("minecraftmoba") && (key.getKey().endsWith("_item") || key.getKey().equals("buffet_serving") || key.getKey().equals("buffet_hotbar")));
    }
    static int used(List<ItemStack> cargo){return (int)cargo.stream().filter(i->i!=null && !i.isEmpty()).count();}
    static int accept(List<ItemStack> cargo,ItemStack input,int capacity){
        if(!eligible(input))return 0;int left=input.getAmount();
        for(var stack:cargo)if(stack.isSimilar(input)){int take=Math.min(left,Math.max(0,stack.getMaxStackSize()-stack.getAmount()));stack.setAmount(stack.getAmount()+take);left-=take;if(left==0)return input.getAmount();}
        while(left>0 && cargo.size()<capacity){int take=Math.min(left,input.getMaxStackSize());cargo.add(input.asQuantity(take));left-=take;}
        return input.getAmount()-left;
    }
    static int collect(Inventory source,List<ItemStack> cargo,int capacity){
        int taken=0;
        for(int i=0;i<source.getSize();i++){var item=source.getItem(i);if(!eligible(item))continue;
            int amount=accept(cargo,item,capacity);if(amount>0){source.setItem(i,amount==item.getAmount()?null:item.asQuantity(item.getAmount()-amount));taken+=amount;}}
        return taken;
    }
    static int deposit(Inventory destination,List<ItemStack> cargo){
        int deposited=0;
        for(var stack:List.copyOf(cargo)){
            var left=destination.addItem(stack.clone());int remains=left.values().stream().mapToInt(ItemStack::getAmount).sum();
            deposited+=stack.getAmount()-remains;if(remains==0)cargo.remove(stack);else stack.setAmount(remains);
        }return deposited;
    }
}
