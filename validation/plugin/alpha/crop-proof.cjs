const { spawn } = require('child_process');
const P='/private/tmp/minecraftmoba-phase1/validation/plugin/protocol/node_modules/';
const mineflayer=require(P+'mineflayer');
const JAVA='/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home/bin/java';
const srv=spawn(JAVA,['-Xmx2G','-jar','paper.jar','--nogui'],{cwd:'/private/tmp/alpha-server',stdio:['pipe','pipe','pipe']});
const log=[]; const say=s=>srv.stdin.write(s+'\n');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
srv.stdout.on('data',d=>log.push(d.toString())); srv.stderr.on('data',d=>log.push(d.toString()));
const waitFor=async(re,t=90000)=>{const t0=Date.now();while(Date.now()-t0<t){if(log.join('').match(re))return true;await sleep(500);}return false;};
const mark=()=>log.length, since=i=>log.slice(i).join('');
(async()=>{
  if(!await waitFor(/Done \(/)){console.log('SERVER FAILED');process.exit(1);}
  await sleep(2000);
  const bot=mineflayer.createBot({host:'127.0.0.1',port:25599,username:'AlphaTester',version:'1.21.11'});
  await new Promise(r=>bot.once('spawn',r));
  say('op AlphaTester'); await sleep(1500);
  say('moba match open'); await sleep(15000);
  say('moba match add AlphaTester north'); await sleep(1500);
  say('moba match start'); await sleep(3000);
  say('gamemode survival AlphaTester'); await sleep(1000);
  let i=mark(); say('moba renew status'); await sleep(2500);
  const c=since(i).match(/ {2}(\S+) kind=(carrots|potatoes) available=(\d+)\/(\d+) at (-?\d+),(-?\d+),(-?\d+)/);
  if(!c){console.log('NO CROP SOURCE');process.exit(0);}
  const [,id,kind,avail,,x,y,z]=c;
  console.log(`target ${id} ${kind} ${avail} at ${x},${y},${z}`);
  say(`tp AlphaTester ${x} ${Number(y)+1} ${z}`);
  await sleep(9000);                       // let the client stream the chunks
  const want = kind==='carrots' ? 'carrots' : 'potatoes';
  let broke=0;
  for(let attempt=0; attempt<25 && broke<10; attempt++){
    const blk = bot.findBlock({matching:b=>b && b.name===want, maxDistance:12});
    if(!blk){ await sleep(800); continue; }
    try{ await bot.dig(blk); broke++; }catch(e){ await sleep(300); }
  }
  console.log('crop blocks broken by the player:', broke);
  await sleep(2500);
  i=mark(); say('moba renew status'); await sleep(2500);
  const after=since(i).match(new RegExp(`  ${id} kind=\\S+ available=(\\d+)/(\\d+)`));
  const hv=since(i).match(/harvests=(\d+) depletions=(\d+) recoveries=(\d+)/);
  console.log(`AFTER: available=${after?after[1]+'/'+after[2]:'?'} (was ${avail}); harvests=${hv?hv[1]:'?'} depletions=${hv?hv[2]:'?'}`);
  // recovery: advance time past recoverTicks (1200) and re-read
  say('moba match skip 2'); await sleep(3000);
  i=mark(); say('moba renew status'); await sleep(2500);
  const rec=since(i).match(new RegExp(`  ${id} kind=\\S+ available=(\\d+)/(\\d+)`));
  const hv2=since(i).match(/harvests=(\d+) depletions=(\d+) recoveries=(\d+)/);
  console.log(`AFTER RECOVERY WINDOW: available=${rec?rec[1]+'/'+rec[2]:'?'}; recoveries=${hv2?hv2[3]:'?'}`);
  say('stop'); await sleep(6000);
  require('fs').writeFileSync('/tmp/crop-proof.log',log.join(''));
  process.exit(0);
})().catch(e=>{console.log('ERR',e.message);process.exit(1);});
