const { spawn } = require('child_process');
const mineflayer = require('/private/tmp/minecraftmoba-phase1/validation/plugin/protocol/node_modules/mineflayer');
const JAVA = '/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home/bin/java';
const srv = spawn(JAVA, ['-Xmx2G','-jar','paper.jar','--nogui'], { cwd:'/private/tmp/alpha-server', stdio:['pipe','pipe','pipe'] });
const log=[]; const say=s=>{console.log('>>> '+s); srv.stdin.write(s+'\n');};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
srv.stdout.on('data',d=>log.push(d.toString())); srv.stderr.on('data',d=>log.push(d.toString()));
const waitFor=async(re,t=90000)=>{const t0=Date.now();while(Date.now()-t0<t){if(log.join('').match(re))return true;await sleep(500);}return false;};
(async()=>{
  if(!await waitFor(/Done \(/)){console.log('SERVER FAILED');srv.kill();process.exit(1);}
  await sleep(2000);
  const bot = mineflayer.createBot({host:'127.0.0.1',port:25599,username:'AlphaTester',version:'1.21.11'});
  await new Promise(r=>bot.once('spawn',r));
  console.log('=== bot joined (unenrolled: NPE check) ===');
  say('op AlphaTester'); await sleep(1500);
  const step=async(c,ms=2500)=>{say(c);await sleep(ms);};
  // MATCH 1
  await step('moba match open',12000);
  await step('moba match add AlphaTester north');
  await step('moba match start',3000);
  await step('moba worksite status');
  await step('moba match skip 7',3000);      // sunset 1 -> real activation
  await step('moba worksite status');
  await step('moba match skip 12',3000);     // sunrise closes, sunset 2 opens
  await step('moba worksite status');
  say('WS_CAPITALIZE_MARKER'); await sleep(200);
  await step('moba worksite list');
  await sleep(500);
  // capitalize whichever is ACTIVATED (parsed below from the list output)
  const m = log.join('').match(/(ws_\d+_\d+)@[-\d,]+ ACTIVATED/);
  if (m) { await step(`moba worksite capitalize ${m[1]} north`); await step('moba worksite status'); }
  else console.log('!!! no ACTIVATED worksite found to capitalize');
  await step('moba match fountain south disable');
  await step('moba match add AlphaTester south');
  await step('moba match kill AlphaTester',3000);
  await step('moba match status');
  // RESET + MATCH 2
  await step('moba match reset',15000);
  say('=== MATCH 2 ==='); await sleep(300);
  await step('moba worksite status');        // must be all DORMANT again
  await step('moba match open',12000);
  await step('moba match add AlphaTester north');
  await step('moba match start',3000);
  await step('moba match status');
  await step('moba worksite status');
  say('stop'); await sleep(6000);
  require('fs').writeFileSync('/tmp/alpha-phase2.log',log.join(''));
  process.exit(0);
})().catch(e=>{console.log('ERR',e.message);require('fs').writeFileSync('/tmp/alpha-phase2.log',log.join(''));process.exit(1);});
