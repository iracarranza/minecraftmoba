// Real protocol activity against an isolated server, not a synthetic food event.
const fs = require('fs'), path = require('path'), {spawn} = require('child_process');
const mineflayer = require('mineflayer');
const [serverArg, reportArg] = process.argv.slice(2);
if (!serverArg || !reportArg || !process.env.JAVA_HOME) throw Error('server, report, JAVA_HOME required');
const server = path.resolve(serverArg), report = path.resolve(reportArg);
if (!fs.existsSync(path.join(server, '.hunger-disposable'))) throw Error('Use prepare.py; disposable marker required');
fs.mkdirSync(report, {recursive:true});
const sleep = ms => new Promise(r => setTimeout(r, ms));
const results = {checks:[], snapshots:[]};
let logs = '', bot;
const srv = spawn(path.join(process.env.JAVA_HOME, 'bin/java'), ['-Xmx2G', '-jar', 'paper.jar', '--nogui'], {cwd:server});
fs.writeFileSync(path.join(report, 'server.log'), '');
for (const stream of [srv.stdout, srv.stderr]) stream.on('data', d => {
  logs += d; fs.appendFileSync(path.join(report, 'server.log'), d);
});
const command = async (c, ms=500) => {srv.stdin.write(c+'\n'); await sleep(ms)};
function check(name, ok, detail) {
  results.checks.push({name, ok, detail}); console.log(JSON.stringify(results.checks.at(-1)));
  if (!ok) throw Error(name);
}
async function until(f, ms=120000) {
  const end=Date.now()+ms;
  while (!f()) {if (Date.now()>end) throw Error('timeout'); await sleep(250)}
}
function snapshot(label) {
  const s={label, food:bot.food, saturation:bot.foodSaturation, health:bot.health,
    bread:bot.inventory.items().filter(i=>i.name==='bread').reduce((n,i)=>n+i.count,0), position:bot.entity.position.clone()};
  results.snapshots.push(s); return s;
}
async function eat() {bot.activateItem(); await sleep(2200); bot.deactivateItem(); await sleep(300)}
async function activity(seconds, stopAtDepletion=false) {
  bot.setControlState('forward',true); bot.setControlState('sprint',true); bot.setControlState('jump',true);
  try {
    for (let i=0;i<seconds;i++) {
      await bot.look((i%8)*Math.PI/4,0,true); await sleep(1000);
      if (stopAtDepletion && bot.food<20) return i+1;
    }
    return seconds;
  } finally {bot.clearControlStates(); await sleep(1000)}
}
(async()=>{try {
  await until(()=>logs.includes('Done (')); await command('moba match open',15000);
  check('Alpha configuration applied', logs.includes("applied map 'resource_light_v2'"));
  bot=mineflayer.createBot({host:'127.0.0.1',port:25640,username:'HungerProbe',auth:'offline',version:'1.21.11'});
  bot.on('error', e=>console.error(e.message));
  await new Promise((r,j)=>{bot.once('spawn',r);bot.once('kicked',j)});
  await command('op HungerProbe'); bot.chat('/moba join'); await sleep(1000);
  await command('moba match add HungerProbe north'); await command('moba match start',2000);
  await command('execute in minecraft:alpha_match run forceload add -2130 -30 -2070 30',2000);
  await command('execute in minecraft:alpha_match run fill -2130 149 -30 -2070 149 30 stone');
  check('test platform built', logs.includes('Successfully filled 3721 block'));
  await command('execute in minecraft:alpha_match run tp HungerProbe -2100 150 0',1000);
  check('standing on test platform', Math.abs(bot.entity.position.y-150)<0.01, bot.entity.position);
  bot.chat('/mobafixture hunger-watch'); await command('give HungerProbe bread 8');
  await bot.equip(bot.inventory.items().find(i=>i.name==='bread'),'hand');
  const full=snapshot('full'); check('unmodified match spawn reserve',full.food===20 && full.saturation===20, full);
  await eat(); const refused=snapshot('full bread attempt');
  check('ordinary food refused while actually full',refused.food===20 && refused.bread===full.bread,refused);
  results.secondsToFirstDepletion=await activity(180,true);
  const depleted=snapshot('natural activity');
  check('activity depletes actual Hunger',depleted.food<20 && depleted.food>0,depleted);
  check('stayed alive away from Fountain',depleted.health===20 && Math.abs(depleted.position.y-150)<0.1,depleted);
  await eat();const fed=snapshot('bread after natural depletion');
  check('ordinary food restores depleted Hunger',fed.food>depleted.food && fed.bread===depleted.bread-1,fed);
  // Isolate the exhaustion rollover too, without waiting through initial saturation.
  bot.chat('/mobafixture hunger-full'); await sleep(500);bot.chat('/mobafixture hunger-drain');await sleep(500);
  await activity(10,true);const rollover=snapshot('zero saturation activity');
  check('vanilla exhaustion rollover reduces food',rollover.food<20,rollover);
  await eat();const refed=snapshot('bread after rollover');
  check('food usable after rollover',refed.food>rollover.food && refed.bread===rollover.bread-1,refed);
  check('food decrease reached event monitor uncancelled',/HUNGER_FOOD_AFTER target=19 cancelled=false/.test(logs));
} catch(e) {results.error=String(e.stack||e);console.error(results.error)} finally {
  if(bot)bot.quit();srv.stdin.write('stop\n');
  if(srv.exitCode===null)await Promise.race([new Promise(r=>srv.once('exit',r)),sleep(20000).then(()=>srv.kill('SIGTERM'))]);
  results.pass=!results.error && results.checks.every(c=>c.ok);
  fs.writeFileSync(path.join(report,'results.json'),JSON.stringify(results,null,2)+'\n');
  process.exitCode=results.pass?0:1;
}})();
