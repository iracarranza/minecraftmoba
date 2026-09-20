const { spawn } = require('child_process');
const mineflayer = require('/private/tmp/minecraftmoba-phase1/validation/plugin/protocol/node_modules/mineflayer');
const JAVA = '/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home/bin/java';
const srv = spawn(JAVA,['-Xmx2G','-jar','paper.jar','--nogui'],{cwd:'/private/tmp/alpha-server',stdio:['pipe','pipe','pipe']});
const log=[]; const say=s=>{srv.stdin.write(s+'\n');};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
srv.stdout.on('data',d=>log.push(d.toString())); srv.stderr.on('data',d=>log.push(d.toString()));
const waitFor=async(re,t=90000)=>{const t0=Date.now();while(Date.now()-t0<t){if(log.join('').match(re))return true;await sleep(500);}return false;};
(async()=>{
  if(!await waitFor(/Done \(/)){console.log('SERVER FAILED');process.exit(1);}
  await sleep(2000);
  const bot=mineflayer.createBot({host:'127.0.0.1',port:25599,username:'AlphaTester',version:'1.21.11'});
  await new Promise(r=>bot.once('spawn',r));
  say('op AlphaTester'); await sleep(1500);
  const step=async(c,ms=2500)=>{say(c);await sleep(ms);};
  await step('moba match open',12000);
  await step('moba match add AlphaTester north');
  await step('moba match start',3000);
  await step('moba match skip 7',3000);         // sunset 1
  const mark=log.length; say('moba worksite list'); await sleep(2500);
  const fresh=log.slice(mark).join('');
  const m=fresh.match(/(ws_\d+_\d+)@[-\d,]+ ACTIVATED/);
  console.log('ACTIVATED found:', m ? m[1] : 'NONE');
  if(m){
    await step(`moba worksite capitalize ${m[1]} north`);
    await step('moba worksite status');
    await step('moba contribution status');
    // second attempt must be refused: first capture is once only
    await step(`moba worksite capitalize ${m[1]} south`);
    // sunrise must NOT revoke a capitalization
    await step('moba match skip 6',3000);
    await step('moba worksite status');
  }
  say('stop'); await sleep(6000);
  require('fs').writeFileSync('/tmp/alpha-cap.log',log.join(''));
  process.exit(0);
})().catch(e=>{console.log('ERR',e.message);require('fs').writeFileSync('/tmp/alpha-cap.log',log.join(''));process.exit(1);});
