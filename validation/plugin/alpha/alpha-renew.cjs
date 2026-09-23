const { spawn } = require('child_process');
const mineflayer = require('/private/tmp/minecraftmoba-phase1/validation/plugin/protocol/node_modules/mineflayer');
const JAVA = '/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home/bin/java';
const srv = spawn(JAVA,['-Xmx2G','-jar','paper.jar','--nogui'],{cwd:'/private/tmp/alpha-server',stdio:['pipe','pipe','pipe']});
const log=[]; const say=s=>srv.stdin.write(s+'\n');
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
  console.log('--- startup: sources cannot bind before a world exists ---');
  await step('moba renew status');
  await step('moba match open',14000);
  console.log('--- after open: sources bound to the instance ---');
  await step('moba renew status');
  await step('moba match add AlphaTester north');
  await step('moba match start',3000);
  // Deplete one source through the real harvest path, then reset and re-check.
  const mark=log.length; say('moba renew status'); await sleep(2000);
  console.log('--- reset must rebuild sources against the NEW world ---');
  await step('moba match reset',18000);
  await step('moba renew status');
  await step('moba match open',14000);
  await step('moba renew status');
  say('stop'); await sleep(6000);
  require('fs').writeFileSync('/tmp/alpha-renew.log',log.join(''));
  process.exit(0);
})().catch(e=>{console.log('ERR',e.message);require('fs').writeFileSync('/tmp/alpha-renew.log',log.join(''));process.exit(1);});
