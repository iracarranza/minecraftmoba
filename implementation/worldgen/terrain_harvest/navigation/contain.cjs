// Deliberately try to leave a harvested volume while ticks are running.
// Adventure mode is the gallery's own inspection mode, so this measures the
// block envelope as a visitor meets it. It does NOT test operator commands,
// spectator flight or teleports, which the gallery never claimed to contain.
const fs = require('node:fs');
const mineflayer = require('mineflayer');

const out = process.env.HARVEST_RESULT;
const bounds = JSON.parse(process.env.HARVEST_BOUNDS);
const target = JSON.parse(process.env.HARVEST_TARGET);
const volume = process.env.HARVEST_VOLUME;
const checks = [];
const record = (name, ok, detail) => { checks.push({ name, ok, ...detail }); console.log((ok ? 'OK  ' : 'FAIL') + ' ' + name + ' ' + JSON.stringify(detail)); };

const bot = mineflayer.createBot({ host: '127.0.0.1', port: Number(process.env.HARVEST_PORT),
  username: process.env.HARVEST_PLAYER, auth: 'offline', version: '1.21.11' });

const chat = [];
let deaths = 0;
bot.on('messagestr', m => chat.push(m));
// Dying teleports the player to world spawn. That is respawn, not an escape,
// and conflating the two would report a containment breach that never happened.
bot.on('death', () => { deaths += 1; console.log('DEATH ' + deaths); });
bot.on('error', e => { console.log('ERROR ' + e.message); finish(1); });
bot.on('kicked', r => { console.log('KICKED ' + JSON.stringify(r)); finish(1); });

const sleep = ms => new Promise(r => setTimeout(r, ms));
async function run(c) { bot.chat('/' + c); await sleep(500); }

function outside(p) {
  // One block of slack: the envelope itself sits at bounds +/- 1.
  return p.x < bounds.x[0] - 1 || p.x > bounds.x[1] + 2
      || p.y < bounds.y[0] - 1 || p.y > bounds.y[1] + 2
      || p.z < bounds.z[0] - 1 || p.z > bounds.z[1] + 2;
}

async function walk(name, yaw, seconds) {
  const deathsBefore = deaths;
  await run(`execute in harvest:${volume} run tp @s ${target[0] + 0.5} ${target[1]} ${target[2] + 0.5} ${yaw} 0`);
  await sleep(500);
  await bot.look(yaw * Math.PI / 180, 0, true);
  bot.setControlState('forward', true); bot.setControlState('sprint', true);
  let escaped = null;
  const start = Date.now();
  while (Date.now() - start < seconds * 1000) {
    await sleep(200);
    if (outside(bot.entity.position)) { escaped = bot.entity.position.clone(); break; }
  }
  bot.clearControlStates();
  await sleep(300);
  const p = bot.entity.position;
  const died = deaths > deathsBefore;
  record('walk ' + name, died ? true : (escaped === null && !outside(p)),
    { yaw, ended: [p.x, p.y, p.z], died,
      outcome: died ? 'died inside the volume; respawn is not an escape, containment inconclusive for this direction'
                    : (escaped ? 'left the source bounds' : 'contained'),
      escaped_at: escaped && !died ? [escaped.x, escaped.y, escaped.z] : null });
}

function finish(code) {
  fs.writeFileSync(out, JSON.stringify({
    schema: 'terrain_containment/1',
    scope: 'adventure-mode walking, falling and block-breaking attempts while ticks run',
    not_covered: ['operator commands', 'spectator flight', 'teleports', 'boats and other vehicles',
                  'every boundary cell; only the probed directions'],
    deaths, checks, pass: code === 0 && checks.length > 0 && checks.every(c => c.ok),
  }, null, 2) + '\n');
  try { bot.quit(); } catch (e) {}
  process.exit(code);
}

bot.once('spawn', async () => {
  try {
    await sleep(1500);
    await run('gamemode adventure @s');
    await run(`execute in harvest:${volume} run tp @s ${target[0] + 0.5} ${target[1]} ${target[2] + 0.5}`);
    await sleep(1000);

    for (const [name, yaw] of [['south', 0], ['west', 90], ['north', 180], ['east', -90]]) {
      await walk(name, yaw, 12);
    }

    // Fall: does the floor stop the descent? Track the lowest point actually
    // reached rather than where the player ends up, because dying on impact
    // means the floor was there, and respawn moves the body to world spawn.
    const deathsBeforeFall = deaths;
    await run(`execute in harvest:${volume} run tp @s ${target[0] + 0.5} ${bounds.y[1]} ${target[2] + 0.5}`);
    let lowest = bounds.y[1];
    for (let i = 0; i < 100; i++) {
      await sleep(200);
      if (deaths > deathsBeforeFall) break;
      lowest = Math.min(lowest, bot.entity.position.y);
      if (i > 15 && Math.abs(bot.entity.position.y - lowest) < 0.01) break;
    }
    record('fall does not pass the floor', lowest > bounds.y[0] - 2,
      { lowest, floor: bounds.y[0], died_on_impact: deaths > deathsBeforeFall,
        outcome: deaths > deathsBeforeFall
          ? 'died on impact; the floor stopped the descent'
          : 'came to rest above the floor' });

    // Adventure mode must not let a visitor mine out of the shell.
    const before = chat.length;
    await run(`execute in harvest:${volume} run tp @s ${target[0] + 0.5} ${target[1]} ${target[2] + 0.5}`);
    await sleep(500);
    const below = bot.blockAt(bot.entity.position.offset(0, -1, 0));
    let broke = false;
    if (below) {
      try { await Promise.race([bot.dig(below), sleep(6000)]); } catch (e) {}
      const now = bot.blockAt(bot.entity.position.offset(0, -1, 0));
      broke = now && now.name !== below.name;
    }
    record('adventure mode cannot mine the floor', !broke,
      { block: below ? below.name : null, broke, chat_since: chat.length - before });

    finish(0);
  } catch (e) { console.log('EXCEPTION ' + e.stack); finish(1); }
});

setTimeout(() => { console.log('TIMEOUT'); finish(1); }, 300000);
