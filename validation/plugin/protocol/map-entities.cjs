const fs=require('node:fs');const mineflayer=require('mineflayer');const {Vec3}=require('vec3');
const bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MOBA_PORT||25576),username:'MobaVerify',auth:'offline',version:'1.21.11'});
fs.mkdirSync('results',{recursive:true});const file=`results/map-entities-${Date.now()}.jsonl`;
const log=(kind,value)=>{const line=JSON.stringify({time:new Date().toISOString(),kind,value});fs.appendFileSync(file,line+'\n');console.log(line)};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));const cmd=async s=>{bot.chat('/'+s);await sleep(300)};
bot.on('messagestr',m=>log('chat',m));bot.on('error',e=>log('error',e.stack));
async function query(command,prefix){return new Promise((resolve,reject)=>{
 const timer=setTimeout(()=>{bot.removeListener('messagestr',onMessage);reject(new Error('Missing assertion response '+command))},3000);
 function onMessage(m){if(m.startsWith(prefix)){clearTimeout(timer);bot.removeListener('messagestr',onMessage);resolve(m)}}
 bot.on('messagestr',onMessage);bot.chat('/'+command);
})}
bot.once('spawn',async()=>{try{
 await cmd('moba setclass MobaVerify none');await cmd('tp @s 200.5 -59 200.5');
 const baseline=await query('data get entity @s equipment','MobaVerify has the following entity data:');
 for(const [name,tag,x] of [['item_frame','moba_frame_fixture',202],['armor_stand','moba_stand_fixture',201]]) {
  await cmd(`kill @e[tag=${tag}]`);
  await cmd(`summon ${name} ${x}.5 -58 200.5 {Tags:["${tag}"],Fixed:1b,NoGravity:1b,ShowArms:1b}`);await sleep(500);
  const target=Object.values(bot.entities).find(e=>e.name===name && e.position.distanceTo(new Vec3(x+.5,-58,200.5))<2);
  if(!target) throw new Error('Missing '+name);
  await bot.lookAt(target.position,true);
  for(const mouse of [2,0]) {
   bot._client.write('use_entity',{target:target.id,mouse,hand:1,sneaking:false,x:0,y:0,z:0,location:new Vec3(0,0,0)});
   await sleep(250);
   const actual=await query('data get entity @s equipment','MobaVerify has the following entity data:');
   if(actual!==baseline) throw new Error('Offhand changed via '+name+' mouse '+mouse);
  }
  const path=name==='item_frame'?'Item.id':'equipment.mainhand.id';
  await query(`execute unless data entity @e[tag=${tag},limit=1] ${path} run say TARGET_EMPTY`, '[MobaVerify] TARGET_EMPTY');
  log('pass','Offhand map cannot enter '+name+' through interact or interact-at');
 }
 // F is intercepted before vanilla hand swapping, in both directions.
 for(let n=0;n<2;n++) {bot._client.write('block_dig',{status:6,location:{x:0,y:0,z:0},face:0,sequence:0});await sleep(200);
  if(await query('data get entity @s equipment','MobaVerify has the following entity data:')!==baseline)throw new Error('F moved map');}
 log('pass','F enter/exit preserves offhand');bot.quit('map entity checks complete');
}catch(e){log('failure',e.stack);bot.quit('map entity checks failed');process.exitCode=1}});
