// Disposable-server acceptance probe. No task recorder or balance experiment.
// NODE_PATH points to the repo's locked validation/plugin/protocol dependencies.
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const [server, report] = process.argv.slice(2);
if (!server || !report || !process.env.JAVA_HOME) throw Error('server report and JAVA_HOME required');
if (path.basename(server) !== 'server' || !server.includes('nearmiss-fixture-930010639')) throw Error('disposable fixture path required');
const build = JSON.parse(fs.readFileSync(path.join(report,'build.json')));
const prep = JSON.parse(fs.readFileSync(path.join(report,'server-preparation.json')));
const results = { scope:'Disposable preflight; admin repositioning is not task traversal evidence', checks:[], snapshots:[] };
let logs = '', bot;
const srv = spawn(path.join(process.env.JAVA_HOME,'bin/java'),['-Xmx3G','-jar','paper.jar','--nogui'],{cwd:server});
const logFile=path.join(report,'server.log'); fs.writeFileSync(logFile,'');
for (const stream of [srv.stdout,srv.stderr]) stream.on('data',d=>{logs+=d;fs.appendFileSync(logFile,d)});
const sleep = ms => new Promise(r=>setTimeout(r,ms));
const say = cmd => {results.snapshots.push({command:cmd});srv.stdin.write(cmd+'\n')};
const step = async (cmd,ms=1000) => {const i=logs.length;say(cmd);await sleep(ms);results.snapshots.push({response:logs.slice(i)});};
const check = (name,ok,detail) => {results.checks.push({name,ok,detail});console.log(JSON.stringify(results.checks.at(-1)));};
async function until(test,ms=120000) {const end=Date.now()+ms;while(Date.now()<end){if(test())return;await sleep(500)}throw Error('timed out');}
async function main() {
 await until(()=>logs.includes('Done ('));
 await step('moba match open',15000);
 check('forced map applied',logs.includes("applied map 'resource_light': "+build.diff.blocks+' blocks'),build.diff);
 check('10 sources bound',logs.includes('[match] bound 10 renewable source(s)'),null);
 await step('moba maps'); await step('moba renew status'); await step('moba worksite status');
 if (!results.checks.every(c=>c.ok)) throw Error('Fixture load/registration failed; no player/task preflight is permitted');
 bot=mineflayer.createBot({host:'127.0.0.1',port:25639,username:'NearPreflight',auth:'offline',version:'1.21.11',respawn:true});
 bot.on('error',e=>console.log('BOT',e.message));
 bot.on('death',()=>results.snapshots.push({event:'client_death'}));
 bot.on('respawn',()=>results.snapshots.push({event:'client_respawn'}));
 await new Promise((resolve,reject)=>{bot.once('spawn',resolve);bot.once('kicked',reject)});
 await step('op NearPreflight'); bot.chat('/moba join'); await sleep(1000);
 await step('moba match add NearPreflight north'); await step('moba match start',2000);
 for (const team of ['north','south']) {
   if(team==='south') await step('moba match add NearPreflight south',2000);
   const h=build.homelands[team].spawn_xzy; const wanted=new Vec3(h[0]+.5,h[2],h[1]+.5);
   check(team+' spawn',bot.entity.position.distanceTo(wanted)<1,{actual:bot.entity.position,wanted,mode:bot.game.gameMode});
   const under=bot.blockAt(wanted.offset(0,-1,0));
   check(team+' fountain support',under?.name==='chiseled_quartz_block',under?.name);
   const from=bot.entity.position.clone(); bot.setControlState('forward',true);bot.setControlState('jump',true);await sleep(2000);bot.clearControlStates();await sleep(500);
   check(team+' can leave spawn',bot.entity.position.distanceTo(from)>2,{from,to:bot.entity.position});
   await step('damage NearPreflight 100 minecraft:generic',1000);
   await step('data get entity NearPreflight Health',250);
   bot.respawn();
   try {await until(()=>bot.entity.position.distanceTo(wanted)<1,10000)} catch (_) {}
   check(team+' respawn',bot.entity.position.distanceTo(wanted)<1,{actual:bot.entity.position,wanted});
 }
 await step('moba match status'); await step('moba work NearPreflight');
 check('Task B has no registered wheat',prep.task_b_wheat_sources.length===0,prep.task_b_wheat_sources);
 await step('moba match reset',15000);
 await step('moba maps'); await step('moba renew status');
 check('static readback ready',build.ready_for_playtest===true,build.readback_failures);
 // No gathering, crafting, source harvesting, task rehearsal or timed trials.
 await step('save-all flush',3000);
}
(async()=>{
 try {await main()} catch(e) {results.error=String(e.stack||e);console.error(results.error)}
 finally {
   if(bot)bot.quit();say('stop');
   if(srv.exitCode===null) await Promise.race([new Promise(r=>srv.once('exit',r)),sleep(30000).then(()=>srv.kill('SIGTERM'))]);
   results.pass=!results.error && results.checks.every(c=>c.ok);
   fs.writeFileSync(path.join(report,'preflight.json'),JSON.stringify(results,null,2)+'\n');
   process.exitCode=results.pass?0:1;
 }
})();
