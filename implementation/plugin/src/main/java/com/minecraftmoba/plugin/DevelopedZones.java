package com.minecraftmoba.plugin;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;
/** Shared physical development footprints. Recognition/progression is separate from raw ecology. */
final class DevelopedZones {
 record Cell(UUID author,String kind){}
 private final Map<BlockAddress,Integer> development=new HashMap<>();
 private final Map<BlockAddress,Cell> cells=new HashMap<>();private final Path file;
 DevelopedZones(MobaPlugin plugin){file=plugin.getDataFolder()==null?null:plugin.getDataFolder().toPath().resolve("developed-zones.yml");if(file!=null&&Files.exists(file)){var y=YamlConfiguration.loadConfiguration(file.toFile());if(y.getInt("version")!=1)throw new IllegalStateException("Unsupported developed footprint version");for(var row:y.getMapList("cells"))cells.put(BlockAddress.decode(row.get("at").toString()),new Cell(UUID.fromString(row.get("author").toString()),row.get("kind").toString()));for(var row:y.getMapList("development"))development.put(BlockAddress.decode(row.get("at").toString()),Integer.parseInt(row.get("value").toString()));}}
 void put(Block b,UUID author,String kind){cells.putIfAbsent(BlockAddress.of(b),new Cell(author,kind));}
 void remove(Block b){cells.remove(BlockAddress.of(b));development.remove(BlockAddress.of(b));}
 boolean contains(BlockAddress at,String kind){var c=cells.get(at);return c!=null&&c.kind.equals(kind);}
 List<BlockAddress> footprint(String kind){return cells.entrySet().stream().filter(e->e.getValue().kind.equals(kind)).map(Map.Entry::getKey).toList();}
 Set<BlockAddress> connected(BlockAddress start,String kind,int cap){return connected(start,kind,cap,b->true);}
 Set<BlockAddress> connected(BlockAddress start,String kind,int cap,java.util.function.Predicate<Block> live){var result=new HashSet<BlockAddress>();var queue=new ArrayDeque<BlockAddress>();queue.add(start);while(!queue.isEmpty()&&result.size()<cap){var at=queue.remove();if(result.contains(at)||!contains(at,kind)||at.block()==null||!live.test(at.block()))continue;result.add(at);queue.addAll(at.adjacent());}return result;}
 UUID author(BlockAddress at){var cell=cells.get(at);return cell==null?null:cell.author();}
 int development(BlockAddress at){return development.getOrDefault(at,0);}
 void develop(BlockAddress at){if(cells.containsKey(at))development.merge(at,1,Integer::sum);}
 void save(){if(file==null)return;var y=new YamlConfiguration();y.set("version",1);var rows=new ArrayList<Map<String,String>>();cells.forEach((at,c)->rows.add(Map.of("at",at.encode(),"author",c.author.toString(),"kind",c.kind)));y.set("cells",rows);y.set("development",development.entrySet().stream().map(e->Map.of("at",e.getKey().encode(),"value",e.getValue().toString())).toList());SupplyLines.write(file,y);}
}
