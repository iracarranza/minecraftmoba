const fs=require('node:fs');
const readline=require('node:readline');
const mineflayer=require('mineflayer');
const {Vec3}=require('vec3');
const scenarios=require('./scenarios');
fs.mkdirSync('results',{recursive:true});
const file=`results/protocol-${Date.now()}.jsonl`;
const log=(kind,value)=>{
  const line=JSON.stringify({time:new Date().toISOString(),kind,value});
  fs.appendFileSync(file,line+'\n'); console.log(line);
};
const bot=mineflayer.createBot({host:'127.0.0.1',port:25576,username:'MobaTest',auth:'offline',version:'1.21.11'});
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));
const cmd=async command=>{bot.chat('/'+command);await sleep(250)};
const action=status=>bot._client.write('block_dig',{status,location:{x:0,y:0,z:0},face:0,sequence:0});
bot.on('messagestr',message=>log('chat',message));
bot.on('error',e=>log('error',e.stack));
bot.on('kicked',reason=>log('kicked',reason));
bot.on('end',reason=>log('end',reason));
bot._client.on('entity_velocity',packet=>{if(packet.entityId===bot.entity?.id) log('velocityPacket',packet)});
bot.once('spawn',()=>log('ready',{scenarios:Object.keys(scenarios),instruction:'Grant op MobaTest in fixture console, then type one scenario name here.'}));
let queue=Promise.resolve();
readline.createInterface({input:process.stdin}).on('line',line=>{
  queue=queue.then(async()=>{
    const name=line.trim();
    if(!Object.hasOwn(scenarios,name)) throw new Error('Unknown scenario: '+name);
    log('scenarioBegin',name);
    await scenarios[name]({bot,cmd,action,sleep,Vec3,log});
    log('scenarioEnd',name);
  }).catch(e=>log('scenarioError',e.stack));
});
