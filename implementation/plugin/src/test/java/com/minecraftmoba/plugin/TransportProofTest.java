package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class TransportProofTest {
 @Test void measuredRepeatabilityIncludesReturnAndLoadingRatherThanOnlyOutboundTime(){
  var p=new TransportProof();var id=UUID.randomUUID();
  assertNull(p.observe(new TransportProof.Delivery(id,5,5,10,40,false)));
  var proof=p.observe(new TransportProof.Delivery(id,5,5,100,130,false));
  assertEquals(5,proof.slots());assertEquals(30,proof.transitTicks());assertEquals(90,proof.intervalTicks());
 }
 @Test void temporaryOrPartialCargoCannotProvePermanentRatedCapacity(){
  var p=new TransportProof();var id=UUID.randomUUID();
  assertNull(p.observe(new TransportProof.Delivery(id,5,4,10,40,false)));
  assertNull(p.observe(new TransportProof.Delivery(id,5,5,50,80,true)));
  assertNull(p.observe(new TransportProof.Delivery(id,5,5,90,120,false)));
 }
 @Test void workersAndChangingMethodsCannotCombineUnrelatedTrips(){
  var p=new TransportProof();var id=UUID.randomUUID();
  assertNull(p.observe(new TransportProof.Delivery(id,5,5,10,40,false)));
  assertNull(p.observe(new TransportProof.Delivery(UUID.randomUUID(),5,5,50,80,false)));
  assertNull(p.observe(new TransportProof.Delivery(id,6,6,90,120,false)));
  assertNull(p.observe(new TransportProof.Delivery(id,6,6,100,140,false)));
 }
}
