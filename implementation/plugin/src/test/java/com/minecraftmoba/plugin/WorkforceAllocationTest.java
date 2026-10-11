package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class WorkforceAllocationTest {
 @Test void commitmentConservesWorkersAndCannotDoubleCountCapacity(){
  var a=new WorkforceAllocation();var owner=UUID.randomUUID();var id=UUID.randomUUID();
  assertTrue(a.add(id,owner,5,1));assertEquals(1,a.active(owner));
  assertTrue(a.commit(id,"line"));assertEquals(0,a.active(owner));assertEquals(5,a.capacity("line"));assertEquals(1,a.count(owner));
  assertFalse(a.commit(id,"other"));assertFalse(a.add(UUID.randomUUID(),owner,5,1));
  assertTrue(a.withdraw(id));assertEquals(1,a.active(owner));assertEquals(0,a.capacity("line"));assertFalse(a.withdraw(id));
  a.remove(id);assertEquals(0,a.count(owner));assertTrue(a.add(UUID.randomUUID(),owner,5,1));
 }
 @Test void limitsArePerOwnerAndRemovalReleasesOnlyItsCommitment(){
  var a=new WorkforceAllocation();var owner=UUID.randomUUID();var other=UUID.randomUUID();var first=UUID.randomUUID();var second=UUID.randomUUID();
  assertFalse(a.add(first,owner,0,3));assertTrue(a.add(first,owner,5,1));assertTrue(a.add(second,other,3,1));
  assertFalse(a.add(first,other,5,2));a.commit(first,"line");a.commit(second,"line");assertEquals(8,a.capacity("line"));
  a.remove(first);assertEquals(3,a.capacity("line"));assertEquals(1,a.count(other));
 }
}
