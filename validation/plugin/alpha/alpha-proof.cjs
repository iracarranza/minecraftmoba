const { spawn } = require('child_process');
const { Vec3 } = require('/private/tmp/minecraftmoba-phase1/validation/plugin/protocol/node_modules/vec3');
const mineflayer = require('/private/tmp/minecraftmoba-phase1/validation/plugin/protocol/node_modules/mineflayer');
const JAVA='/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home/bin/java';
const srv=spawn(JAVA,['-Xmx2G','-jar','paper.jar','--nogui'],{cwd:'/private/tmp/alpha-server',stdio:['pipe','pipe','pipe']});
const log=[]; const say=s=>srv.stdin.write(s+'\n');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
srv.stdout.on('data',d=>log.push(d.toString())); srv.stderr.on('data',d=>log.push(d.toString()));
const waitFor=async(re,t=90000)=>{const t0=Date.now();while(Date.now()-t0<t){if(log.join('').match(re))return true;await sleep(500);}return false;};
const mark=()=>log.length; const since=i=>log.slice(i).join('');
(async()=>{
  if(!await waitFor(/Done \(/)){console.log('SERVER FAILED');process.exit(1);}
  await sleep(2000);
  const bot=mineflayer.createBot({host:'127.0.0.1',port:25599,username:'AlphaTester',version:'1.21.11'});
  await new Promise(r=>bot.once('spawn',r));
  say('op AlphaTester'); await sleep(1500);
  const step=async(c,ms=2500)=>{say(c);await sleep(ms);};
  await step('moba match open',14000);
  await step('moba match add AlphaTester north');
  await step('moba match start',3000);
  await step('gamemode creative AlphaTester',1500);

  // ---- RENEWABLE: a CROP source, broken by the player (BlockBreakEvent) ----
  let i=mark(); say('moba renew status'); await sleep(2500);
  const crop=since(i).match(/ {2}(\S+) kind=(carrots|potatoes|wheat) available=(\d+)\/(\d+) at (-?\d+),(-?\d+),(-?\d+)/);
  console.log('CROP SOURCE BEFORE:', crop?crop[0].trim():'NONE');
  if(crop){
    const [,id,,avail,,x,y,z]=crop;
    say(`tp AlphaTester ${x} ${Number(y)+1} ${z}`); await sleep(2500);
    // Break crop blocks as the player, which is the real harvest path.
    let broke=0;
    for(let dx=-3;dx<=3 && broke<8;dx++){
      for(let dz=-3;dz<=3 && broke<8;dz++){
        const bx=Number(x)+dx, bz=Number(z)+dz, by=Number(y);
        const blk=bot.blockAt(new Vec3(bx,by,bz));
        if(blk && /wheat|carrot|potato/.test(blk.name)){
          try{ await bot.dig(blk); broke++; }catch(e){}
        }
      }
    }
    console.log('crop blocks broken by player:', broke);
    await sleep(2000);
    i=mark(); say('moba renew status'); await sleep(2500);
    const m=since(i).match(new RegExp(`  ${id} kind=\\S+ available=(\\d+)/(\\d+)`));
    const h=since(i).match(/harvests=(\d+) depletions=(\d+)/);
    console.log(`CROP AFTER: available=${m?m[1]:'?'}/${m?m[2]:'?'} (was ${avail}); harvests=${h?h[1]:'?'} depletions=${h?h[2]:'?'}`);
  }

  // ---- INFRA: issued BY THE PLAYER, not the console ----
  bot.chat('/moba infra enter'); await sleep(2500);
  i=mark(); say('moba route'); await sleep(2000);
  console.log('INFRA AFTER PLAYER COMMAND:', (since(i).match(/routes=\d+ pendingDesignations=\d+ inInfraMode=\d+/)||['?'])[0]);

  // ---- WORKSITE + VICTORY + RESET ----
  await step('moba match skip 7',3000);
  i=mark(); say('moba worksite list'); await sleep(2500);
  const ws=since(i).match(/(ws_\d+_\d+)@[-\d,]+ ACTIVATED/);
  if(ws) await step(`moba worksite capitalize ${ws[1]} north`);
  await step('moba match fountain south disable');
  await step('moba match add AlphaTester south');
  await step('moba match kill AlphaTester',3000);
  i=mark(); await step('moba match reset',20000);
  console.log('RESET:', (since(i).match(/Match reset:.*/)||['?'])[0]);

  await step('moba match open',14000);
  await step('moba match add AlphaTester north');
  await step('moba match start',3000);
  i=mark(); say('moba renew status'); say('moba route'); say('moba worksite status'); await sleep(3000);
  const m2=since(i);
  console.log('M2 RENEW:', (m2.match(/renewable sources=\d+ harvests=\d+ depletions=\d+ recoveries=\d+/)||['?'])[0]);
  console.log('M2 ROUTES/INFRA:', (m2.match(/routes=\d+ pendingDesignations=\d+ inInfraMode=\d+/)||['?'])[0]);
  console.log('M2 WORKSITES:', (m2.match(/\{DORMANT=\d+.*\}/)||['?'])[0]);
  if(ws){ await step('moba match skip 7',3000);
    i=mark(); say(`moba worksite capitalize ${ws[1]} north`); await sleep(2000);
    console.log('M2 RECAPITALIZE:', (since(i).match(/ws_\S+ .*/)||['?'])[0]); }
  say('stop'); await sleep(6000);
  require('fs').writeFileSync('/tmp/alpha-proof.log',log.join(''));
  process.exit(0);
})().catch(e=>{console.log('ERR',e.message);require('fs').writeFileSync('/tmp/alpha-proof.log',log.join(''));process.exit(1);});
