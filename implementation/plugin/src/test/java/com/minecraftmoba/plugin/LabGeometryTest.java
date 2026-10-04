package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LabGeometryTest {
    static class Terrain implements LabGeometry.Terrain {
        final Map<LabGeometry.XZ,Integer> heights = new HashMap<>();
        final Set<LabGeometry.Pos> protectedBlocks = new HashSet<>();
        final Set<LabGeometry.XZ> water = new HashSet<>();
        public int height(int x,int z) { return heights.getOrDefault(new LabGeometry.XZ(x,z),64); }
        public boolean water(int x,int z) { return water.contains(new LabGeometry.XZ(x,z)); }
        public String block(LabGeometry.Pos p) { return p.y()<=height(p.x(),p.z()) ? "minecraft:stone" : "minecraft:air"; }
        public boolean protectedAt(LabGeometry.Pos p) { return protectedBlocks.contains(p); }
    }
    private List<LabGeometry.TemplateBlock> platform() {
        return List.of(new LabGeometry.TemplateBlock(-1,-1,0,"minecraft:stone_bricks"),
                new LabGeometry.TemplateBlock(0,-1,0,"minecraft:stone_bricks"),
                new LabGeometry.TemplateBlock(1,-1,0,"minecraft:stone_bricks"));
    }
    @Test void flatStructureFitsActualColumnsWithoutInventingCircularPad() {
        var plan=LabGeometry.structure(new Terrain(),new LabGeometry.XZ(0,0),platform(),false);
        assertTrue(plan.accepted());assertEquals(3,plan.edits().keySet().stream().map(p->new LabGeometry.XZ(p.x(),p.z())).distinct().count());assertEquals(0,plan.relief());
        assertFalse(plan.edits().containsKey(new LabGeometry.Pos(0,64,1)));
    }
    @Test void maximumEarthworkRejectsASevereColumnEvenIfAveragePasses() {
        var terrain=new Terrain();terrain.heights.put(new LabGeometry.XZ(1,0),80);
        var plan=LabGeometry.structure(terrain,new LabGeometry.XZ(0,0),platform(),false);
        assertFalse(plan.accepted());assertTrue(plan.maxCut()>12);
        assertTrue(plan.problems().stream().anyMatch(s->s.contains("Local cut/fill")));
    }
    @Test void unevenFollowPolicyIsPreviewOnlyForRigidBuildings() {
        var terrain=new Terrain();terrain.heights.put(new LabGeometry.XZ(1,0),65);
        var plan=LabGeometry.structure(terrain,new LabGeometry.XZ(0,0),platform(),true);
        assertFalse(plan.accepted());assertEquals("minecraft:stone_bricks",plan.edits().get(new LabGeometry.Pos(1,65,0)));
    }
    @Test void protectedBlocksAreRejectedBeforeAnyWorldWrites() {
        var terrain=new Terrain();terrain.protectedBlocks.add(new LabGeometry.Pos(0,64,0));
        assertFalse(LabGeometry.structure(terrain,new LabGeometry.XZ(0,0),platform(),false).accepted());
    }
    @Test void corridorReroutesAroundCliffRatherThanCarvingThroughIt() {
        var terrain=new Terrain();for(int z=-2;z<=2;z++)terrain.heights.put(new LabGeometry.XZ(3,z),90);
        var line=LabGeometry.corridor(terrain,new LabGeometry.XZ(0,0),new LabGeometry.XZ(6,0));
        assertTrue(line.stream().anyMatch(p->Math.abs(p.z())>=3));
        assertTrue(line.stream().noneMatch(p->p.x()==3&&Math.abs(p.z())<=2));
    }
    @Test void profilePreservesAnchorHeightsAndRejectsImpossibleTransition() {
        var out=LabGeometry.profile(new int[]{64,80,64});
        assertEquals(64,out[0]);assertEquals(64,out[2]);assertTrue(out[1]<=65);
        var terrain=new Terrain();terrain.heights.put(new LabGeometry.XZ(1,0),68);
        var plan=LabGeometry.path(terrain,new LabGeometry.XZ(0,0),new LabGeometry.XZ(1,0));
        assertFalse(plan.accepted());assertTrue(plan.problems().stream().anyMatch(s->s.contains("Endpoint")));
    }
    @Test void slopingPathHasDirectionalStairModules() {
        var terrain=new Terrain();for(int x=-12;x<=16;x++)for(int z=-12;z<=12;z++)terrain.heights.put(new LabGeometry.XZ(x,z),64+Math.max(0,Math.min(x,4)));
        var plan=LabGeometry.path(terrain,new LabGeometry.XZ(0,0),new LabGeometry.XZ(4,0));
        assertTrue(plan.accepted(),plan.problems().toString());assertTrue(plan.steps()>0);
        assertTrue(plan.edits().values().stream().anyMatch(s->s.contains("stairs[facing=east")));
    }
    @Test void pathPlannerEnforcesBoundBeforeSamplingTerrain() {
        assertThrows(IllegalArgumentException.class,()->LabGeometry.path(new Terrain(),new LabGeometry.XZ(0,0),new LabGeometry.XZ(193,0)));
    }
    @Test void accessDistinguishesUnreachableFromMeasuredSymmetry() {
        int[] height={64,64,64,64,64,64,64,64,64};boolean[] water=new boolean[9];
        assertEquals(16,LabAccess.cost(height,water,3,0,2));
        height[1]=height[4]=height[7]=Integer.MIN_VALUE;
        assertEquals(Double.POSITIVE_INFINITY,LabAccess.cost(height,water,3,0,2));
        assertEquals(0,LabAccess.asymmetry(100,100));assertEquals(.4,LabAccess.asymmetry(80,120),1e-9);
    }
    @Test void undoRestoresOverlappingEditsInLifoOrderAndRetainsFailedRestore() {
        var journal=new LabUndo<String>();var restored=new ArrayList<String>();
        journal.push("first",List.of("old A"),List.of());
        journal.push("second",List.of("new A","old B"),List.of());
        assertThrows(IllegalStateException.class,()->journal.undo(s->{throw new IllegalStateException("write failure");},id->{}));
        assertEquals(2,journal.depth());
        assertEquals("second",journal.undo(restored::add,id->{}));
        assertEquals(List.of("old B","new A"),restored);
        assertEquals("first",journal.undo(restored::add,id->{}));assertEquals("old A",restored.getLast());
        assertThrows(IllegalStateException.class,()->journal.undo(restored::add,id->{}));
    }
    @Test void undoRemovesOnlySpawnedMembers() {
        UUID authored=UUID.randomUUID();var removed=new ArrayList<UUID>();var journal=new LabUndo<String>();
        journal.push("swarm",List.of(),List.of(authored));journal.undo(s->{},removed::add);
        assertEquals(List.of(authored),removed);
    }
}
