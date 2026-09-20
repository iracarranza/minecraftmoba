// Drive the complete Alpha lifecycle: console commands plus one real player.
const { spawn } = require('child_process');
const mineflayer = require('/private/tmp/minecraftmoba-phase1/validation/plugin/protocol/node_modules/mineflayer');
const JAVA = '/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home/bin/java';

const srv = spawn(JAVA, ['-Xmx2G', '-jar', 'paper.jar', '--nogui'],
  { cwd: '/private/tmp/alpha-server', stdio: ['pipe', 'pipe', 'pipe'] });
const log = [];
const say = s => { console.log('>>> ' + s); srv.stdin.write(s + '\n'); };
const sleep = ms => new Promise(r => setTimeout(r, ms));
srv.stdout.on('data', d => { const t = d.toString(); log.push(t); process.stdout.write(t.split('\n').filter(l=>/moba|match|Done|ERROR|Exception/i.test(l)).map(l=>l+'\n').join('')); });
srv.stderr.on('data', d => log.push(d.toString()));

const waitFor = async (re, timeout=90000) => {
  const t0 = Date.now();
  while (Date.now() - t0 < timeout) {
    if (log.join('').match(re)) return true;
    await sleep(500);
  }
  return false;
};

(async () => {
  if (!await waitFor(/Done \(/)) { console.log('SERVER FAILED TO START'); srv.kill(); process.exit(1); }
  console.log('=== server up ===');
  await sleep(2000);

  const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25599, username: 'AlphaTester', version: '1.21.11' });
  await new Promise(r => bot.once('spawn', r));
  console.log('=== bot joined ===');
  say('op AlphaTester'); await sleep(1000);

  const step = async (cmd, ms=2500) => { say(cmd); await sleep(ms); };

  await step('moba match status');
  await step('moba match open', 12000);          // P0.2 load instance from template
  await step('moba match add AlphaTester north');// P0.3 team assignment
  await step('moba match start', 4000);          // P0.4 bootstrap + homeland spawn
  await step('moba match status');
  await step('moba match skip 7', 4000);         // P0.5 crosses sunset 1 at 6m
  await step('moba match skip 12', 4000);        // sunrise + sunset 2
  await step('moba match skip 24', 6000);        // sunsets 3 and 4
  await step('moba match status');
  // P0.6 exercise the REAL predicate: needs an opposing team with a disabled
  // fountain and no survivors. Add a second participant to be eliminated.
  await step('moba match add AlphaTester south');// reassign to south for the test
  await step('moba match fountain south disable');
  await step('moba match status');
  await step('moba match kill AlphaTester', 3000); // now predicate should fire
  await step('moba match status');
  await step('moba match reset', 15000);         // P0.7 clear + restore world
  await step('moba match status');
  say('stop');
  await sleep(6000);
  require('fs').writeFileSync('/tmp/alpha-lifecycle.log', log.join(''));
  process.exit(0);
})().catch(e => { console.log('ERR', e.message); require('fs').writeFileSync('/tmp/alpha-lifecycle.log', log.join('')); process.exit(1); });
